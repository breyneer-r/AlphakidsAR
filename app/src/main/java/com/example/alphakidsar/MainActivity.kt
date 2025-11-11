package com.example.alphakidsar

import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.ImageFormat
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var modelRenderable: ModelRenderable? = null
    private val isDetecting = AtomicBoolean(false)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnchorNode: AnchorNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        load3DModel()

        // Procesar cada frame de la cámara AR
        arFragment.arSceneView.scene.addOnUpdateListener {
            val frame = arFragment.arSceneView.arFrame
            if (frame != null) {
                detectTextInFrame(frame)
            }
        }
    }

    /** Crea un cubo azul como modelo 3D */
    private fun load3DModel() {
        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.BLUE))
            .thenAccept { material ->
                modelRenderable = ShapeFactory.makeCube(
                    Vector3(0.1f, 0.1f, 0.1f),
                    Vector3.zero(),
                    material
                )
            }
            .exceptionally { throwable ->
                Log.e("ARCore", "Error al crear el modelo 3D: ${throwable.message}")
                Toast.makeText(this, "Error al crear modelo 3D", Toast.LENGTH_LONG).show()
                null
            }
    }

    /** Detecta texto en el frame actual y coloca un objeto si se detecta la letra A */
    private fun detectTextInFrame(frame: Frame) {
        if (isDetecting.get() || modelRenderable == null) return

        try {
            val cameraImage = frame.acquireCameraImage()
            if (cameraImage.format != ImageFormat.YUV_420_888) {
                cameraImage.close()
                return
            }

            val bitmap = yuvToBitmap(cameraImage)
            val image = InputImage.fromBitmap(bitmap, 0)
            cameraImage.close()

            isDetecting.set(true)

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    for (block in visionText.textBlocks) {
                        val detectedText = block.text.trim().uppercase()

                        if (detectedText.contains("A")) {
                            Log.d("MLKit", "¡Letra A detectada!")

                            if (lastAnchorNode != null) return@addOnSuccessListener

                            val boundingBox = block.boundingBox
                            if (boundingBox != null) {
                                val centerX = boundingBox.centerX().toFloat()
                                val centerY = boundingBox.centerY().toFloat()

                                val hitResults = frame.hitTest(centerX, centerY)
                                val anchor = hitResults.firstOrNull()?.createAnchor()

                                if (anchor != null) {
                                    placeObject(anchor)
                                } else {
                                    Log.w("ARCore", "No se encontró plano AR en la ubicación detectada.")
                                }
                            }
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Error procesando texto: ${e.message}")
                }
                .addOnCompleteListener {
                    isDetecting.set(false)
                }

        } catch (e: Exception) {
            Log.e("ARCore", "No se pudo capturar la imagen: ${e.message}")
        }
    }

    /** Convierte la imagen YUV_420_888 del frame en un Bitmap ARGB8888 */
    private fun yuvToBitmap(image: Image): Bitmap {
        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 90, out)
        val jpegBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    /** Coloca el modelo 3D en la escena */
    private fun placeObject(anchor: Anchor) {
        lastAnchorNode?.anchor?.detach()
        lastAnchorNode?.setParent(null)

        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)

        val modelNode = Node()
        modelNode.setParent(anchorNode)
        modelNode.renderable = modelRenderable
        modelNode.localScale = Vector3(0.5f, 0.5f, 0.5f)

        lastAnchorNode = anchorNode
        Toast.makeText(this, "Objeto 3D colocado en AR", Toast.LENGTH_LONG).show()
    }
}
