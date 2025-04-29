import type { DirectionVector } from './types';

const cachedTrigonometricValue = new Map<
  number,
  { cos: number; sin: number }
>();

function getCachedTrigonometricValue(radian: number) {
  if (cachedTrigonometricValue.has(radian)) {
    return cachedTrigonometricValue.get(radian) as {
      cos: number;
      sin: number;
    };
  }

  const value = {
    cos: Math.cos(radian),
    sin: Math.sin(radian),
  };

  if (cachedTrigonometricValue.size < 1000) {
    cachedTrigonometricValue.set(radian, value);
  }

  return value;
}

export function getNarrowedDirectionVector(
  normalizedAnchor: DirectionVector,
  normalizedTarget: DirectionVector,
  maxRadian: number,
) {
  const dot =
    normalizedAnchor.x * normalizedTarget.x +
    normalizedAnchor.y * normalizedTarget.y; // cosθ = anchor · target (when |anchor|=|target|=1)
  const cross =
    normalizedAnchor.x * normalizedTarget.y -
    normalizedAnchor.y * normalizedTarget.x; // sinθ = anchor × target, the sign of the cross product determines the rotation direction

  const maxRadianTrigonometricValue = getCachedTrigonometricValue(maxRadian);

  if (dot >= maxRadianTrigonometricValue.cos) return normalizedTarget; // when the actual angle is less than or equal to the maximum allowed angle, return directly

  // construct the parameters of the rotation matrix (θ = maxRadian)
  const cos = maxRadianTrigonometricValue.cos; // the cosθ term of the rotation matrix
  const sin = maxRadianTrigonometricValue.sin * (cross > 0 ? 1 : -1); // determine the rotation direction based on the sign of the cross product

  // apply the rotation matrix [cos, -sin; sin, cos] to the anchor vector
  return {
    x: normalizedAnchor.x * cos - normalizedAnchor.y * sin, // the new x component
    y: normalizedAnchor.x * sin + normalizedAnchor.y * cos, // the new y component
  };
}
