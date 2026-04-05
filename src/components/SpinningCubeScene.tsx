import { useCallback, useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView } from 'expo-gl';
import type { ExpoWebGLRenderingContext } from 'expo-gl';
import { Renderer } from 'expo-three';
import * as THREE from 'three';

type Props = {
  height: number;
  onFps?: (fps: number) => void;
};

function nowMs(): number {
  const p = globalThis.performance;
  return typeof p?.now === 'function' ? p.now() : Date.now();
}

export function SpinningCubeScene({ height, onFps }: Props) {
  const rafRef = useRef<number | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (rafRef.current != null) {
        cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
    };
  }, []);

  const onContextCreate = useCallback(
    async (gl: ExpoWebGLRenderingContext) => {
      const { drawingBufferWidth: width, drawingBufferHeight: h } = gl;
      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(70, width / h, 0.1, 1000);
      camera.position.z = 2.4;

      const geometry = new THREE.BoxGeometry(1, 1, 1);
      const material = new THREE.MeshBasicMaterial({ color: 0x00ffe5 });
      const cube = new THREE.Mesh(geometry, material);
      scene.add(cube);

      const renderer = new Renderer({ gl });
      renderer.setSize(width, h);

      let frames = 0;
      let lastTick = nowMs();

      const loop = () => {
        if (!mountedRef.current) return;
        rafRef.current = requestAnimationFrame(loop);
        const t = nowMs();
        cube.rotation.x = t * 0.0009;
        cube.rotation.y = t * 0.0011;
        renderer.render(scene, camera);
        gl.endFrameEXP();

        frames++;
        const elapsed = t - lastTick;
        if (elapsed >= 500) {
          const fps = (frames / elapsed) * 1000;
          onFps?.(fps);
          frames = 0;
          lastTick = t;
        }
      };
      rafRef.current = requestAnimationFrame(loop);
    },
    [onFps]
  );

  return (
    <View style={[styles.wrap, { height }]}>
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    width: '100%',
    backgroundColor: '#05080c',
    borderRadius: 12,
    overflow: 'hidden',
  },
  gl: { flex: 1 },
});
