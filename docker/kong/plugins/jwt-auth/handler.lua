-- Custom JWT Auth Plugin for Kong Gateway
-- Replicates the Spring Cloud Gateway JwtAuthFilter behavior:
--   1. Extracts Bearer token from Authorization header
--   2. Validates HS256 signature using shared secret
--   3. Checks expiration claim
--   4. Injects X-User-Id and X-User-Email headers (from "sub" claim)
--
-- This avoids needing Kong's built-in JWT plugin (which requires iss claim + Consumer registration).

local cjson = require "cjson.safe"
local openssl_hmac = require "resty.openssl.hmac"
local ngx_encode_base64 = ngx.encode_base64
local ngx_decode_base64 = ngx.decode_base64

local plugin = {
  PRIORITY = 1000,  -- Run before most other plugins
  VERSION  = "1.0.0",
}


-- ─── Helpers ──────────────────────────────────────────────────────────────────

--- Base64-URL decode (JWT uses URL-safe base64 without padding)
local function base64url_decode(input)
  -- Replace URL-safe characters with standard base64 characters
  local reminder = #input % 4
  if reminder > 0 then
    input = input .. string.rep("=", 4 - reminder)
  end
  input = input:gsub("-", "+"):gsub("_", "/")
  return ngx_decode_base64(input)
end

--- Base64-URL encode (for HMAC comparison)
local function base64url_encode(input)
  local result = ngx_encode_base64(input)
  result = result:gsub("+", "-"):gsub("/", "_"):gsub("=", "")
  return result
end

--- Split a JWT string into its 3 parts
local function split_token(token)
  local parts = {}
  for part in token:gmatch("[^%.]+") do
    table.insert(parts, part)
  end
  if #parts ~= 3 then
    return nil, "invalid JWT structure (expected 3 parts)"
  end
  return parts
end


-- ─── Access Phase ─────────────────────────────────────────────────────────────

function plugin:access(conf)
  -- 1. Extract Authorization header
  local auth_header = kong.request.get_header("Authorization")
  if not auth_header then
    return kong.response.exit(401, { message = "Missing Authorization header" })
  end

  -- 2. Check "Bearer " prefix
  if not auth_header:find("^Bearer ") then
    return kong.response.exit(401, { message = "Invalid Authorization format. Expected: Bearer <token>" })
  end

  local token = auth_header:sub(8) -- strip "Bearer "

  -- 3. Split token into header.payload.signature
  local parts, err = split_token(token)
  if not parts then
    return kong.response.exit(401, { message = "Invalid token: " .. err })
  end

  local header_b64  = parts[1]
  local payload_b64 = parts[2]
  local signature_b64 = parts[3]

  -- 4. Decode header to verify algorithm
  local header_json = base64url_decode(header_b64)
  if not header_json then
    return kong.response.exit(401, { message = "Failed to decode token header" })
  end

  local header = cjson.decode(header_json)
  if not header then
    return kong.response.exit(401, { message = "Failed to parse token header" })
  end

  -- Support HS256 and HS384 (jjwt library auto-selects based on key length)
  local alg_map = {
    HS256 = "SHA256",
    HS384 = "SHA384",
  }
  local sha_alg = alg_map[header.alg]
  if not sha_alg then
    return kong.response.exit(401, { message = "Unsupported algorithm: " .. (header.alg or "unknown") .. ". Supported: HS256, HS384." })
  end

  -- 5. Verify signature using the shared secret
  local signing_input = header_b64 .. "." .. payload_b64

  -- The secret is decoded identically to Java's Base64.getDecoder().decode(secret).
  -- Java's decoder is lenient about missing '=' padding, while nginx's
  -- ngx.decode_base64 is strict. We add padding to match Java's behavior.
  local padded_secret = conf.jwt_secret
  local pad_remainder = #padded_secret % 4
  if pad_remainder > 0 then
    padded_secret = padded_secret .. string.rep("=", 4 - pad_remainder)
  end
  local secret_decoded = ngx_decode_base64(padded_secret)
  if not secret_decoded then
    kong.log.err("Failed to Base64-decode jwt_secret. Value length=", #conf.jwt_secret)
    return kong.response.exit(500, { message = "Internal server error" })
  end

  local computed_hmac, hmac_err = openssl_hmac.new(secret_decoded, sha_alg)
  if not computed_hmac then
    kong.log.err("Failed to create HMAC: ", hmac_err)
    return kong.response.exit(500, { message = "Internal server error" })
  end

  computed_hmac:update(signing_input)
  local digest = computed_hmac:final()
  local expected_signature = base64url_encode(digest)

  if expected_signature ~= signature_b64 then
    kong.log.err("JWT SIG MISMATCH: alg=", header.alg, " sha=", sha_alg,
                 " secret_len=", #secret_decoded,
                 " expected=", expected_signature,
                 " actual=", signature_b64)
    return kong.response.exit(401, { message = "Invalid token signature" })
  end

  -- 6. Decode and validate payload
  local payload_json = base64url_decode(payload_b64)
  if not payload_json then
    return kong.response.exit(401, { message = "Failed to decode token payload" })
  end

  local payload = cjson.decode(payload_json)
  if not payload then
    return kong.response.exit(401, { message = "Failed to parse token payload" })
  end

  -- 7. Check expiration
  if payload.exp then
    local now = ngx.time()
    if now > payload.exp then
      return kong.response.exit(401, { message = "Token has expired" })
    end
  end

  -- 8. Extract claims and inject as downstream headers
  --    JWT payload contains: sub (email), userId (numeric ID), role (ADMIN/CUSTOMER)
  --    KongHeaderAuthFilter requires: X-User-Id, X-User-Role, X-Consumer-Username
  local subject = payload.sub
  if not subject or subject == "" then
    return kong.response.exit(401, { message = "Token missing 'sub' claim" })
  end

  -- Inject X-User-Email (always the email from 'sub' claim)
  kong.service.request.set_header("X-User-Email", subject)

  -- Inject X-User-Id (numeric userId from custom claim, fallback to sub/email)
  local user_id = payload.userId
  if user_id then
    kong.service.request.set_header("X-User-Id", tostring(user_id))
  else
    kong.service.request.set_header("X-User-Id", subject)
  end

  -- Inject X-User-Role (from custom 'role' claim: ADMIN, CUSTOMER, etc.)
  local role = payload.role
  if role then
    kong.service.request.set_header("X-User-Role", role)
  end

  -- Inject X-Consumer-Username (used by KongHeaderAuthFilter as principal)
  kong.service.request.set_header("X-Consumer-Username", subject)

  kong.log.debug("JWT validated for user: ", subject, " role: ", role or "unknown")
end


return plugin
