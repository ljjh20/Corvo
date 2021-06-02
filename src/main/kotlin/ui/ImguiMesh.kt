package ui

import glm_.L
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.backends.SceneryWindow
import graphics.scenery.backends.ShaderType
import graphics.scenery.textures.Texture
import graphics.scenery.ui.MenuNode
import graphics.scenery.xtradimensionvr.XPlot
import imgui.*
import imgui.classes.Context
import imgui.impl.glfw.ImplGlfw
import kool.*
import kool.lib.fill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil.*
import uno.glfw.GlfwWindow

lateinit var imguiMesh: ImguiMesh

fun topLevel() {

}

class ImguiMesh(hub: Hub, val plot: XPlot) : Mesh("Menu") {

    var showDemoWindow = false

    var hideMe = true

    // Setup Dear ImGui context
    val ctx = Context()

    val vrTexSize: Vec2i? = null

    /** for passing inputs in vr */
    var vrCursorPos: Vec2? = null

    // 1f unless you use any scaling != 100%, 125% corresponds to 1.25f
    val scaleFont = 1.75f

    // this must come before retrieving the texture (which comes right after, in the init)
    val font = ImGui.io.fonts.addFontFromFileTTF("graphics/scenery/ui/ProggyClean.ttf", 16f * scaleFont)!!

    val fontMap: Texture = ImGui.io.fonts.getTexDataAsRGBA32().let { (pixels, size, _) ->
        Texture(dimensions = Vector3i(size.x, size.y, 1), contents = pixels)
    }

    var vtx = ByteBuffer(1024)
    var idx = IntBuffer(256)

    val window = GlfwWindow.from((hub.get<Renderer>()!!.window as SceneryWindow.GLFWWindow).window)
    val menus = mutableMapOf<String, MenuNode>() // TODO switch to Int key

    // Setup Platform/Renderer bindings
    val implGlfw = ImplGlfw(window, true, vrTexSize)

    val checkboxes = BooleanArray(20) { it % 2 == 0 }

    init {
        println("new ImguiMesh")
        imguiMesh = this
    }

    override fun preDraw(): Boolean {
        //        if(!stale) {
        //            return true
        //        }

        // setup time step and input states
        //        implGlfwNewFrame()
        implGlfw.newFrame()

        //        ImGui.run {

        ImGui.newFrame()

        // use this on high resolution monitors
        //            dsl.withFont(this@Imgui.font) {
        //        ImGui.showDemoWindow(::showDemoWindow)
        //                ImGui.setNextWindowSize(size)
//        println("hideMe=$hideMe")
        if (hideMe) {
            dsl.window("Annotations") {
                for (i in plot.annotationList.indices) {
                    val ann = plot.annotationList[i]
                    if (ImGui.checkbox(ann, checkboxes, i)) {
                        println("selected $i")

//                        plot.annKeyList[plot.annotationPicker].visible = false
//                        plot.labelList[plot.annotationPicker].visible = false
//
//                        plot.annotationPicker = i
//
//                        plot.annKeyList[plot.annotationPicker].visible = true
//                        plot.labelList[plot.annotationPicker].visible = true
//
//                        GlobalScope.launch(Dispatchers.Default) {
//                            plot.updateInstancingColor()
//                        }
                    }
                }
                //                }
            }
        }
        //        }

        return renderImgui()
    }

    fun renderImgui(): Boolean {

        // Rendering
        ImGui.render()
        val drawData = ImGui.drawData!!

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        val fbSize = Vec2i(drawData.displaySize * drawData.framebufferScale)
        if (fbSize anyLessThanEqual 0)
            return false

        // Setup scale and translation:
        // Our visible imgui space lies from draw_data->DisplayPps (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right). DisplayPos is (0,0) for single viewport apps.
        val scale = Vector2f(2f / drawData.displaySize.x, 2f / drawData.displaySize.y)
        val translate = Vector2f(-1f - drawData.displayPos.x * scale[0], -1f - drawData.displayPos.y * scale[1])

        //        logger.info("Got ${drawData.cmdLists.size} draw cmd lists")

        val DrawVertSize = Vec2.size * 2 + Int.SIZE_BYTES
        val vertexSize = drawData.totalVtxCount * DrawVertSize
        val indexSize = drawData.totalIdxCount

        //        println("vertexSize: $vertexSize, indexSize: $indexSize")

        if (vertexSize > vtx.cap) {
            vtx.free()
            vtx = ByteBuffer(vertexSize * 2) // give it some space
        }
        if (indexSize > idx.capacity()) {
            idx.free()
            idx = IntBuffer(indexSize * 2)
        }
        var vtxPtr = vtx.adr
        var idxPtr = idx.adr

        //        run {
        //            val i = 0
        //            val o = DrawVertSize * i
        //            println("DrawVert[$i] { pos(${vtx.getFloat(o)}, ${vtx.getFloat(o + Float.BYTES)}, " +
        //                            "uv(${vtx.getFloat(o + Vec2.size)}, ${vtx.getFloat(o + Vec2.size + Float.BYTES)} " +
        //                            "col(${vtx.getInt(o + Vec2.size * 2)}")
        //        }

        vtx.fill(0)
        idx.fill(0)

        // retain our still existing menus
        menus.entries.retainAll { (k, _) -> drawData.cmdLists.any { it._ownerName == k } }

        var globalVtxOffset = 0
        var globalIdxOffset = 0
        drawData.cmdLists.forEach { drawList ->
            val node = menus.getOrPut(drawList._ownerName) {
                logger.info("Adding new node for ${drawList._ownerName}")
                MenuNode(drawList._ownerName).also {
                    it.vertexSize = 2
                    it.material = ShaderMaterial.fromClass(MenuNode::class.java, listOf(ShaderType.VertexShader, ShaderType.FragmentShader))
                    it.material.textures["sTexture"] = fontMap
                    it.material.blending.transparent = true
                    it.material.blending.setOverlayBlending()
                    it.material.cullingMode = Material.CullingMode.None
                    it.material.depthTest = Material.DepthTest.Always
                    addChild(it)
                }
            }

            node.splitDrawCalls.clear()

            val clipOff = drawData.displayPos         // (0,0) unless using multi-viewports
            val clipScale = drawData.framebufferScale // (1,1) unless using retina display which are often (2,2)
            for (cmd in drawList.cmdBuffer) {
                val clipRect = Vec4 { (cmd.clipRect[it] - clipOff[it % 2]) * clipScale[it % 2] }

                if (clipRect.x < fbSize.x && clipRect.y < fbSize.y && clipRect.z >= 0f && clipRect.w >= 0f) {
                    // Negative offsets are illegal for vkCmdSetScissor
                    if (clipRect.x < 0f)
                        clipRect.x = 0f
                    if (clipRect.y < 0f)
                        clipRect.y = 0f

                    // Apply scissor/clipping rectangle
                    node.uScale = scale
                    node.uTranslate = translate
                    val extent = Vector2i((clipRect.z - clipRect.x).i, (clipRect.w - clipRect.y).i)
                    val offset = Vector2i(clipRect.x.i, clipRect.y.i)
                    val scissor = Scissor(extent, offset)

                    node.splitDrawCalls += DrawState(cmd.elemCount, cmd.idxOffset + globalIdxOffset, cmd.vtxOffset + globalVtxOffset, scissor)
                }
            }
            globalIdxOffset += drawList.idxBuffer.rem
            globalVtxOffset += drawList.vtxBuffer.rem

            memCopy(drawList.vtxBuffer.data.adr, vtxPtr, drawList.vtxBuffer.size * DrawVertSize.L)
            memCopy(memAddress(drawList.idxBuffer), idxPtr, drawList.idxBuffer.remaining() * Integer.BYTES.L)
            vtxPtr += drawList.vtxBuffer.size * DrawVertSize.L
            idxPtr += drawList.idxBuffer.remaining() * Integer.BYTES

            node.vertices = vtx.asFloatBuffer()
            //node.normals = node.vertices // TODO change me
            node.indices = idx

            node.dirty = true
        }
        //        println("vtx:$vtx")
        //        println("idx:$idx")
        //        val duration = System.nanoTime() - start
        //        logger.info("Imgui serialisation took ${duration / 10e6}ms")
        //logger.info("menu children: ${this.children.joinToString { "${it.name}, ${(it as? MenuNode)?.vertices?.remaining()}, ${(it as? MenuNode)?.splitDrawCalls?.size}" }}")
        return true
    }
}