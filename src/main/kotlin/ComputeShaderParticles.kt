import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import java.io.File

fun main() = application {
  configure {
    fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
  }
  program {
    val computeWidth = 1000
    val computeHeight = 1
    val particleCount = computeWidth * computeHeight
    val computeShader = ComputeShader.fromFile(
      File("src/main/glsl/particle-computer.glsl")
    )

    val rt = renderTarget(width, height) {
      colorBuffer(type = ColorType.FLOAT16, format = ColorFormat.RGB)
    }
    // -- create the vertex buffer
    val geometry = vertexBuffer(vertexFormat {
      position(3)
    }, 4)

    // -- fill the vertex buffer with vertices for a unit quad
    geometry.put {
      write(Vector3(-1.0, -1.0, 0.0))
      write(Vector3(-1.0, 1.0, 0.0))
      write(Vector3(1.0, -1.0, 0.0))
      write(Vector3(1.0, 1.0, 0.0))
    }

    // -- create the secondary vertex buffer, which will hold particle transformations
    val transformsBuffer = vertexBuffer(vertexFormat {
      attribute("transform", VertexElementType.MATRIX44_FLOAT32)
    }, particleCount)

    // FIXME would it be possible to somehow hold it in the transformsBuffer?
    // -- create the tertiary vertex buffer, which will hold particle properties
    val propertiesBuffer = vertexBuffer(vertexFormat {
      attribute("velocity", VertexElementType.VECTOR2_FLOAT32)
    }, particleCount)

    // -- fill the initial transform buffer
    transformsBuffer.put {
      for (i in 0 until particleCount) {
        write(transform {
          translate(Math.random() * width, Math.random() * height)
          rotate(Vector3.UNIT_Z, Math.random() * 360.0)
          //scale(30.0 + Math.random() * 80.0)
          scale(50.0 + Math.random() * 10)
        })
      }
    }

    propertiesBuffer.put {
      for (i in 0 until particleCount) {
        // velocity
        write(Vector2((Math.random() * .1 - .5), Math.random() * .1 - .5))
      }
    }
    computeShader.uniform("computeWidth", computeWidth)
    computeShader.uniform("width", width.toDouble())
    computeShader.uniform("height", height.toDouble())
    computeShader.buffer("transformsBuffer", transformsBuffer)
    computeShader.buffer("propertiesBuffer", propertiesBuffer)
    extend {
      drawer.isolated {
        fill = ColorRGBa.PINK.opacify(.5)
        shadeStyle = shadeStyle {
          vertexTransform = "x_viewMatrix = x_viewMatrix * i_transform;"
          // FIXME assuming that I have my custom fragmentTransform, is there anyway to pass particle properties buffer to it?
          fragmentTransform = """

#define SCALE 30.0
// rotation speed, might be negat ive to spin counter-clockwise
#define ROTATION_SPEED -5.0

#define INTENSITY_PULSE_SPEED .3
            float iTime = p_time;
           float dist = length(v_worldPosition);
           float angle = atan(v_worldPosition.x, v_worldPosition.y);
           float newCol = (
sin(
      (dist * SCALE)
      + angle
      + (cos(dist * SCALE))
      - (iTime * ROTATION_SPEED)
  )
      - dist * (2.3 + sin(iTime * INTENSITY_PULSE_SPEED))
      + 0.3
           ) * smoothstep(1, .9, dist) * 2.;
         newCol = clamp(newCol, 0, 1);
         //float factor = (dist + sin(p_time * 3. + angle * 15) * .002);
         //float factor *= smoothstep(1, .9, factor);
         x_fill.rgb = x_fill.rgb * newCol;
         x_fill.a = newCol;
        """
          parameter("time", seconds)
        }
        vertexBufferInstances(
          listOf(geometry), listOf(transformsBuffer), DrawPrimitive.TRIANGLE_STRIP, particleCount
        )
      }
      computeShader.uniform("time", seconds)
      computeShader.execute(computeWidth, computeHeight)
      //drawer.image(rt.colorBuffer(0))
    }
  }
}
