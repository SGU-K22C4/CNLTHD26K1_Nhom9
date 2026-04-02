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

  if header.alg ~= "HS256" then
    return kong.response.exit(401, { message = "Unsupported algorithm: " .. (header.alg or "unknown") .. ". Only HS256 is supported." })
  end

  -- 5. Verify signature using the shared secret
  local signing_input = header_b64 .. "." .. payload_b64

  -- The secret is Base64-encoded (same as Spring Boot's Keys.hmacShaKeyFor(Base64.decode(secret)))
  local secret_decoded = ngx_decode_base64(conf.jwt_secret)
  if not secret_decoded then
    kong.log.err("Failed to Base64-decode jwt_secret. Check your configuration.")
    return kong.response.exit(500, { message = "Internal server error" })
  end

  local computed_hmac, hmac_err = openssl_hmac.new(secret_decoded, "SHA256")
  if not computed_hmac then
    kong.log.err("Failed to create HMAC: ", hmac_err)
    return kong.response.exit(500, { message = "Internal server error" })
  end

  computed_hmac:update(signing_input)
  local digest = computed_hmac:final()
  local expected_signature = base64url_encode(digest)

  if expected_signature ~= signature_b64 then
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

  -- 8. Extract subject (email) and inject as downstream headers
  --    Matches Spring Gateway JwtAuthFilter behavior:
  --      headers.set("X-User-Id", claims.getSubject())
  --      headers.set("X-User-Email", claims.getSubject())
  local subject = payload.sub
  if not subject or subject == "" then
    return kong.response.exit(401, { message = "Token missing 'sub' claim" })
  end

  kong.service.request.set_header("X-User-Id", subject)
  kong.service.request.set_header("X-User-Email", subject)

  kong.log.debug("JWT validated successfully for user: ", subject)
end


return plugin
