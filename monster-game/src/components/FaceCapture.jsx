import { useRef, useState, useEffect } from 'react'
import { extractColorsFromImage } from '../utils/monsterGenerator'
import './FaceCapture.css'

export function FaceCapture({ onCapture, onSkip }) {
  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const [stream, setStream] = useState(null)
  const [error, setError] = useState(null)
  const [captured, setCaptured] = useState(false)

  useEffect(() => {
    let mediaStream = null
    const startCamera = async () => {
      try {
        mediaStream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'user', width: 640, height: 480 },
        })
        setStream(mediaStream)
        if (videoRef.current) {
          videoRef.current.srcObject = mediaStream
        }
      } catch (err) {
        setError('Could not access camera. You can skip and use random generation.')
      }
    }
    startCamera()
    return () => mediaStream?.getTracks().forEach((t) => t.stop())
  }, [])

  const captureFace = () => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas || !stream) return

    const ctx = canvas.getContext('2d')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight

    ctx.drawImage(video, 0, 0)

    const centerX = canvas.width / 2
    const centerY = canvas.height / 2
    const size = Math.min(canvas.width, canvas.height) * 0.6
    const x = (centerX - size / 2)
    const y = (centerY - size / 2)

    const faceRegion = ctx.getImageData(x, y, size, size)
    const colors = extractColorsFromImage(faceRegion)

    stream.getTracks().forEach((t) => t.stop())
    setStream(null)
    setCaptured(true)

    onCapture({
      colors,
      faceImageData: canvas.toDataURL('image/png'),
    })
  }

  const handleSkip = () => {
    stream?.getTracks().forEach((t) => t.stop())
    setStream(null)
    onSkip()
  }

  return (
    <div className="face-capture">
      <h2>Capture Your Face</h2>
      <p className="subtitle">Position your face in the circle. Your features will shape your monster!</p>

      {error && <p className="error">{error}</p>}

      <div className="camera-container">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="camera-video"
        />
        <div className="face-guide" />
        <canvas ref={canvasRef} style={{ display: 'none' }} />
      </div>

      <div className="capture-actions">
        <button className="btn btn-primary" onClick={captureFace} disabled={!stream}>
          Capture
        </button>
        <button className="btn btn-secondary" onClick={handleSkip}>
          Skip (Random Monster)
        </button>
      </div>
    </div>
  )
}
