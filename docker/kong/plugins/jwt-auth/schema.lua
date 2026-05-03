-- Schema definition for the custom jwt-auth plugin
-- Defines the configuration fields that can be set in kong.yml

local typedefs = require "kong.db.schema.typedefs"

return {
  name = "jwt-auth",
  fields = {
    { consumer = typedefs.no_consumer },  -- This plugin does not bind to Kong consumers
    { protocols = typedefs.protocols_http },
    {
      config = {
        type = "record",
        fields = {
          {
            -- The Base64-encoded HMAC secret used to verify JWT signatures.
            -- Must match the JWT_SECRET used by the Spring Boot user-service.
            jwt_secret = {
              type = "string",
              required = true,
            },
          },
        },
      },
    },
  },
}
