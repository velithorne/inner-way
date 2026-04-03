/** DECODE blueprint — room geometry from acoustic impulse response */

export type RoomGeometry = {
  width: number;
  height: number;
  depth: number;
  confidence: number;
  isOpen: boolean;
  resonanceFreqs: number[];
  impulseResponse: Float32Array;
};
