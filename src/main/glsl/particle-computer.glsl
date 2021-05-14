#version 430
// FIXME why 16?
layout(local_size_x = 16, local_size_y = 16) in;

uniform int computeWidth;
uniform float width;
uniform float height;
uniform float time;

struct ParticleTransform {
  mat4 transform;
};

struct ParticleProperties {
  vec2 velocity;
};

layout(binding=0) buffer transformsBuffer {
  ParticleTransform transforms[];
};

layout(binding=1) buffer propertiesBuffer {
  ParticleProperties properties[];
};

void main() {
  // FIXME is offset calculated correctly?
  const uint offset = gl_GlobalInvocationID.x + gl_GlobalInvocationID.y * computeWidth;
  ParticleTransform pt = transforms[offset];
  ParticleProperties pp = properties[offset];
  vec2 position = vec2(pt.transform[3][0], pt.transform[3][1]);
  if ((position.x < 0) || (position.x > width)) {
    properties[offset].velocity *= vec2(-1, 1);
  }
  if ((position.y < 0) || (position.y > height)) {
    properties[offset].velocity *= vec2(1, -1);
  }
  position += properties[offset].velocity;
  transforms[offset].transform[3][0] = position.x;
  transforms[offset].transform[3][1] = position.y;
  properties[offset].velocity += vec2(
  sin(time * 10.01 + float(offset) * .001),
  cos(time * 10.01 + float(offset) * .001)
  ) * .000001;
}
