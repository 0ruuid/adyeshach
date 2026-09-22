package ink.ptms.adyeshach.core.entity.path

import ink.ptms.adyeshach.core.AdyeshachSettings
import ink.ptms.adyeshach.core.entity.type.errorBy
import ink.ptms.adyeshach.core.util.FoliaRuntime
import ink.ptms.adyeshach.core.util.runOnRegion
import org.bukkit.Location
import org.bukkit.util.Consumer
import org.bukkit.util.Vector
import taboolib.common.io.digest
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.function.warning
import taboolib.common5.util.getStackTraceString
import taboolib.module.navigation.NodeEntity
import taboolib.module.navigation.PathSmoothing
import taboolib.module.navigation.RandomPositionGenerator
import taboolib.module.navigation.createPathfinder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author sky
 * @since 2020-08-13 16:31
 */
object PathFinderHandler {

    val tracker = ConcurrentHashMap<String, Int>()
    private val warnedSyncPathfinderOnFolia = AtomicBoolean()

    /**
     * 请求寻路路径
     *
     * @param start 起点
     * @param target 终点
     * @param path 路径类型
     * @param call 回调函数
     */
    fun requestNavigation(start: Location, target: Location = start, path: PathType = PathType.WALK_2, call: Consumer<ResultNavigation>) {
        return request(start, target, path, Request.NAVIGATION) { call.accept(it as ResultNavigation) }
    }

    /**
     * 请求寻路路径
     *
     * @param start 起点
     * @param target 终点
     * @param path 路径类型
     * @param call 回调函数
     */
    fun requestRandomPosition(start: Location, target: Location = start, path: PathType = PathType.WALK_2, call: Consumer<ResultRandomPosition>) {
        return request(start, target, path, Request.RANDOM_POSITION) { call.accept(it as ResultRandomPosition) }
    }

    /**
     * 请求寻路路径
     *
     * @param start 起点
     * @param target 终点
     * @param path 路径类型
     * @param request 请求方式
     * @param call 回调函数
     */
    fun request(start: Location, target: Location = start, path: PathType = PathType.WALK_2, request: Request = Request.NAVIGATION, call: Consumer<Result>) {
        // 世界判断
        if (start.world!!.name != target.world!!.name) {
            errorBy("error-different-worlds")
        }
        // 记录请求信息
        if (AdyeshachSettings.debug) {
            val traceString = PathfinderRequestTracker().getStackTraceString()
            submitAsync {
                val traceStringCode = traceString.digest("sha-1")
                tracker[traceStringCode] = tracker.computeIfAbsent(traceStringCode) { 0 } + 1
                var str = ""
                str += "Request times: ${tracker[traceStringCode]}, Path: $path, Request: $request\n"
                str += "World: ${start.world!!.name}, Start: ${start.toVector()}, End: ${target.toVector()}\n"
                str += "Trace:\n"
                str += traceString
                newFile(getDataFolder(), "logs/pathfinder/${traceStringCode}.log").writeText(str)
            }
        }
        // 请求发起时间
        val startTime = System.currentTimeMillis()
        // Folia 的 GlobalRegionScheduler 不能同步等待区域任务；寻路始终在异步线程计算。
        val isFolia = FoliaRuntime.isFolia
        val async = FoliaRuntime.shouldPathfindAsync(AdyeshachSettings.pathfinderSync)
        if (isFolia && AdyeshachSettings.pathfinderSync && warnedSyncPathfinderOnFolia.compareAndSet(false, true)) {
            warning("Folia detected: Settings.pathfinder-sync=true is ignored; pathfinding runs asynchronously.")
        }
        submit(async = async) {
            // 请求开始时间
            val scheduleTime = System.currentTimeMillis()
            // 寻路请求
            if (request == Request.NAVIGATION) {
                val nodeEntity = NodeEntity(start, path.height, path.width)
                val pathFinder = createPathfinder(nodeEntity)
                // 最大 32 格的寻路请求
                val findPath = pathFinder.findPath(target, distance = 64f)
                // 调试模式下将显示路径节点
                if (AdyeshachSettings.debug && !isFolia) {
                    val display = Runnable { findPath?.nodes?.forEach { it.display(target.world!!) } }
                    display.run()
                }
                // TabooLib PathSmoothing 会在一个 region 中横向读取多个路径点；
                // Folia 下跨 region 路径不能这样访问世界，保留原始路径节点。
                val pointList = if (findPath != null) {
                    if (isFolia) {
                        findPath.nodes.map { Vector(it.x + 0.5, it.y.toDouble(), it.z + 0.5) }.toMutableList()
                    } else {
                        PathSmoothing.smooth(findPath, nodeEntity).toMutableList()
                    }
                } else {
                    ArrayList()
                }
                if (pointList.isNotEmpty()) {
                    // 如果路径的最后一个点不是目的地
                    val last = pointList.last()
                    if (last.blockX != target.blockX || last.blockZ != target.blockZ) {
                        // 如果高度相同，距离为 1
                        if (last.blockY == target.blockY && last.distance(Vector(target.blockX, target.blockY, target.blockZ)) == 1.0) {
                            // 添加目的地（居中）
                            pointList.add(Vector(target.blockX + 0.5, target.blockY.toDouble(), target.blockZ + 0.5))
                        }
                    }
                }
                // 调用回调函数
                dispatchResult(start, call, ResultNavigation(pointList, startTime, scheduleTime))
            } else {
                var vec: Vector? = null
                // 重复最多 10 次的游荡请求
                repeat(10) {
                    if (vec == null) {
                        vec = RandomPositionGenerator.generateLand(NodeEntity(start, path.height, path.width), 10, 7)
                    }
                }
                if (vec != null) {
                    dispatchResult(start, call, ResultRandomPosition(vec!!, startTime, scheduleTime))
                }
            }
        }
    }

    private fun dispatchResult(start: Location, call: Consumer<Result>, result: Result) {
        start.clone().runOnRegion { call.accept(result) }
    }
}

class PathfinderRequestTracker : Exception()
