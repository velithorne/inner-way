import { Renderer } from 'expo-three';
import { useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { GLView } from 'expo-gl';
import * as THREE from 'three';

import type { RoomGeometry } from '../types/decode';

type Props = {
  geometry: RoomGeometry | null;
};

const COLOR = 0x001850;
const BREATH_MS = 4000;

export function AcousticWireframe({ geometry }: Props) {
  const geomRef = useRef(geometry);
  const sceneApiRef = useRef<{
    rebuild: (g: RoomGeometry | null) => void;
  } | null>(null);
  const cancelledRef = useRef(false);

  useEffect(() => {
    cancelledRef.current = false;
    return () => {
      cancelledRef.current = true;
      sceneApiRef.current = null;
    };
  }, []);

  useEffect(() => {
    geomRef.current = geometry;
    sceneApiRef.current?.rebuild(geometry);
  }, [geometry]);

  const onContextCreate = async (gl: ExpoWebGLRenderingContext) => {
    const renderer = new Renderer({ gl });
    renderer.setSize(gl.drawingBufferWidth, gl.drawingBufferHeight);

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(
      75,
      gl.drawingBufferWidth / gl.drawingBufferHeight,
      0.1,
      100
    );
    camera.position.z = 4;
    scene.add(new THREE.AmbientLight(0xffffff, 0.6));

    const extras: THREE.Object3D[] = [];
    let mesh: THREE.LineSegments | null = null;
    let hemi: THREE.LineSegments | null = null;

    const mat = () =>
      new THREE.LineBasicMaterial({
        color: COLOR,
        transparent: true,
        opacity: 0.4,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });

    const clear = () => {
      if (mesh) {
        scene.remove(mesh);
        mesh.geometry.dispose();
        (mesh.material as THREE.Material).dispose();
        mesh = null;
      }
      if (hemi) {
        scene.remove(hemi);
        hemi.geometry.dispose();
        (hemi.material as THREE.Material).dispose();
        hemi = null;
      }
      for (const o of extras) {
        scene.remove(o);
        if (o instanceof THREE.Mesh) {
          o.geometry.dispose();
          (o.material as THREE.Material).dispose();
        }
      }
      extras.length = 0;
    };

    const rebuild = (g: RoomGeometry | null) => {
      clear();
      const m = mat();
      if (!g || g.isOpen) {
        const geo = new THREE.SphereGeometry(3, 24, 12, 0, Math.PI * 2, 0, Math.PI / 2);
        const wire = new THREE.WireframeGeometry(geo);
        hemi = new THREE.LineSegments(wire, m);
        hemi.rotation.x = Math.PI;
        scene.add(hemi);
      } else {
        const { width, height, depth } = g;
        const box = new THREE.BoxGeometry(width, height, depth);
        const wire = new THREE.WireframeGeometry(box);
        mesh = new THREE.LineSegments(wire, m);
        scene.add(mesh);

        const cornerGeo = new THREE.SphereGeometry(0.02, 8, 8);
        const cornerMat = new THREE.MeshBasicMaterial({ color: 0x4fc3f7 });
        const corners: [number, number, number][] = [
          [-width / 2, -height / 2, -depth / 2],
          [width / 2, -height / 2, -depth / 2],
          [-width / 2, height / 2, -depth / 2],
          [width / 2, height / 2, -depth / 2],
          [-width / 2, -height / 2, depth / 2],
          [width / 2, -height / 2, depth / 2],
          [-width / 2, height / 2, depth / 2],
          [width / 2, height / 2, depth / 2],
        ];
        for (const [x, y, z] of corners) {
          const c = new THREE.Mesh(cornerGeo.clone(), cornerMat);
          c.position.set(x, y, z);
          scene.add(c);
          extras.push(c);
        }
      }
    };

    sceneApiRef.current = { rebuild };
    rebuild(geomRef.current);

    const start = performance.now();
    const loop = () => {
      if (cancelledRef.current) return;
      const t = (performance.now() - start) / BREATH_MS;
      const breath = 1 + 0.008 * Math.sin(t * Math.PI * 2);
      if (mesh) mesh.scale.setScalar(breath);
      if (hemi) hemi.scale.setScalar(breath);
      renderer.render(scene, camera);
      gl.endFrameEXP();
      requestAnimationFrame(loop);
    };
    requestAnimationFrame(loop);
  };

  return (
    <View style={styles.wrap} pointerEvents="none">
      <GLView style={styles.gl} onContextCreate={onContextCreate} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    ...StyleSheet.absoluteFillObject,
  },
  gl: {
    flex: 1,
    backgroundColor: 'transparent',
  },
});

type ExpoWebGLRenderingContext = Parameters<
  NonNullable<React.ComponentProps<typeof GLView>['onContextCreate']>
>[0];
