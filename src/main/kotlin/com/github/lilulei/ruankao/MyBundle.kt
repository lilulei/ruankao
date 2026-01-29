package com.github.lilulei.ruankao

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * 国际化资源包常量，指定资源文件路径
 */
@NonNls
private const val BUNDLE = "messages.MyBundle"

/**
 * 国际化消息资源束对象，提供多语言支持功能
 * 继承自DynamicBundle，支持动态加载国际化消息
 */
object MyBundle : DynamicBundle(BUNDLE) {

    /**
     * 获取格式化的消息字符串
     *
     * @param key 资源键名，用于查找对应的消息模板
     * @param params 消息格式化参数，用于替换消息模板中的占位符
     * @return 格式化后的本地化消息字符串
     */
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    /**
     * 获取延迟加载的消息指针
     * 用于在运行时动态获取本地化消息，支持延迟计算
     *
     * @param key 资源键名，用于查找对应的消息模板
     * @param params 消息格式化参数，用于替换消息模板中的占位符
     * @return 延迟加载的消息对象
     */
    @Suppress("unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
