export const PROTOCOL_VERSION = 1

export function isSupportedPacket(payload) {
  return payload && payload.protocolVersion === PROTOCOL_VERSION
}
