package com.huanchengfly.tieba.post.utils

import android.util.Log
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern

object BlockManager {
    private const val TAG = "block"
    private val blockList: MutableList<Block> = mutableListOf()

    val blackList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_BLACK_LIST }

    val whiteList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_WHITE_LIST }

    private fun List<Block>.dumpBlocks(name: String) {
        Log.d(TAG, "=== $name ($size entries) ===")
        forEachIndexed { i, b ->
            val kw = b.getKeywords().joinToString(", ")
            Log.d(TAG, "  [$i] id=${b.id} type=${b.type}(${typeName(b.type)}) cat=${b.category} regex=${b.isRegex} keywords=[$kw] user=${b.username} uid=${b.uid}")
        }
    }

    private fun typeName(type: Int): String = when (type) {
        Block.TYPE_KEYWORD -> "keyword"
        Block.TYPE_USER -> "user"
        else -> "unknown($type)"
    }

    suspend fun addBlock(block: Block): Block {
        val id = DatabaseUtil.insertBlock(block)
        val savedBlock = block.copy(id = id)
        blockList.add(savedBlock)
        Log.d(TAG, "addBlock: id=$id category=${savedBlock.category} type=${typeName(savedBlock.type)} keywords=[${savedBlock.getKeywords().joinToString(", ")}] regex=${savedBlock.isRegex} user=${savedBlock.username} uid=${savedBlock.uid}")
        blackList.dumpBlocks("blackList after add")
        whiteList.dumpBlocks("whiteList after add")
        return savedBlock
    }

    fun addBlockAsync(
        block: Block,
        callback: ((Boolean) -> Unit)? = null,
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val id = DatabaseUtil.insertBlock(block)
            val savedBlock = block.copy(id = id)
            blockList.add(savedBlock)
            Log.d(TAG, "addBlockAsync: id=$id category=${savedBlock.category} type=${typeName(savedBlock.type)} keywords=[${savedBlock.getKeywords().joinToString(", ")}] regex=${savedBlock.isRegex} user=${savedBlock.username} uid=${savedBlock.uid}")
            blackList.dumpBlocks("blackList after addAsync")
            whiteList.dumpBlocks("whiteList after addAsync")
            callback?.invoke(true)
        }
    }

    suspend fun removeBlock(id: Long) {
        Log.d(TAG, "removeBlock: id=$id")
        DatabaseUtil.deleteBlockById(id)
        blockList.removeAll { it.id == id }
        blackList.dumpBlocks("blackList after remove")
        whiteList.dumpBlocks("whiteList after remove")
    }

    suspend fun init() {
        // Keep idempotent for "re-init" after imports.
        blockList.clear()
        blockList.addAll(DatabaseUtil.getAllBlocks())
        Log.d(TAG, "init: loaded ${blockList.size} blocks")
        blackList.dumpBlocks("blackList")
        whiteList.dumpBlocks("whiteList")
    }

    fun shouldBlock(content: String): Boolean {
        val truncated = if (content.length > 200) content.take(200) + "..." else content
        Log.d(TAG, ">> shouldBlock(content) len=${content.length} preview=[$truncated]")

        var anyWhiteMatch = false
        val isWhite = whiteList.any { block ->
            block.type == Block.TYPE_KEYWORD && block.getKeywords().any { keyword ->
                val matched = if (block.isRegex) {
                    try {
                        Pattern.compile(keyword).matcher(content).find()
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    content.contains(keyword)
                }
                Log.d(TAG, "  whitelist id=${block.id} keyword=[$keyword] regex=${block.isRegex} matched=$matched")
                if (matched) anyWhiteMatch = true
                matched
            }
        }
        if (isWhite) {
            Log.d(TAG, "<< shouldBlock(content) -> false (whitelist matched)")
            return false
        }
        var anyBlackMatch = false
        val isBlack = blackList.any { block ->
            block.type == Block.TYPE_KEYWORD && block.getKeywords().any { keyword ->
                val matched = if (block.isRegex) {
                    try {
                        Pattern.compile(keyword).matcher(content).find()
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    content.contains(keyword)
                }
                Log.d(TAG, "  blacklist id=${block.id} keyword=[$keyword] regex=${block.isRegex} matched=$matched")
                if (matched) anyBlackMatch = true
                matched
            }
        }
        Log.d(TAG, "<< shouldBlock(content) -> $isBlack")
        return isBlack
    }

    fun shouldBlock(userId: Long = 0L, userName: String? = null, userNameShow: String? = null): Boolean {
        Log.d(TAG, ">> shouldBlock(user) userId=$userId userName=$userName userNameShow=$userNameShow")

        var anyWhiteUserMatch = false
        val isWhiteUser = whiteList.any { block ->
            !block.isRegex &&
                block.type == Block.TYPE_USER &&
                (block.uid == userId.toString() || block.username == userName || block.username == userNameShow).also { matched ->
                    if (matched) anyWhiteUserMatch = true
                    Log.d(TAG, "  whitelist user id=${block.id} uid=${block.uid} username=${block.username} matched=$matched")
                }
        }
        if (isWhiteUser) {
            Log.d(TAG, "<< shouldBlock(user) -> false (whitelist user matched)")
            return false
        }

        val names = listOfNotNull(userName, userNameShow)
        names.forEach { name ->
            Log.d(TAG, "  checking name=[$name] against keyword blocks")
        }
        val shouldBlockByName = names.any { it.isNotBlank() && shouldBlock(it) }
        if (shouldBlockByName) {
            Log.d(TAG, "<< shouldBlock(user) -> true (keyword matched username)")
            return true
        }

        var anyBlackUserMatch = false
        val isBlackUser = blackList.any { block ->
            !block.isRegex &&
                block.type == Block.TYPE_USER &&
                (block.uid == userId.toString() || block.username == userName || block.username == userNameShow).also { matched ->
                    if (matched) anyBlackUserMatch = true
                    Log.d(TAG, "  blacklist user id=${block.id} uid=${block.uid} username=${block.username} matched=$matched")
                }
        }
        Log.d(TAG, "<< shouldBlock(user) -> $isBlackUser")
        return isBlackUser
    }

    fun ThreadInfo.shouldBlock(): Boolean {
        val titlePreview = if (title.length > 100) title.take(100) + "..." else title
        val absPreview = if (abstractText.length > 100) abstractText.take(100) + "..." else abstractText
        Log.d(TAG, ">> ThreadInfo.shouldBlock() title=[$titlePreview] abstract=[$absPreview] authorId=$authorId author=${author?.name}")
        val result = shouldBlock(title) || shouldBlock(abstractText) || shouldBlock(
            authorId.takeIf { it != 0L } ?: (author?.id ?: -1),
            author?.name,
            author?.nameShow,
        )
        Log.d(TAG, "<< ThreadInfo.shouldBlock() -> $result")
        return result
    }

    fun Post.shouldBlock(): Boolean {
        val plain = content.plainText
        val preview = if (plain.length > 100) plain.take(100) + "..." else plain
        Log.d(TAG, ">> Post.shouldBlock() floor=${floor} plainText=[$preview] author_id=$author_id author=${author?.name}")
        val result = shouldBlock(plain) || shouldBlock(
            author_id.takeIf { it != 0L } ?: (author?.id ?: -1),
            author?.name,
            author?.nameShow,
        )
        Log.d(TAG, "<< Post.shouldBlock() -> $result")
        return result
    }

    fun SubPostList.shouldBlock(): Boolean {
        val plain = content.plainText
        val preview = if (plain.length > 100) plain.take(100) + "..." else plain
        Log.d(TAG, ">> SubPostList.shouldBlock() id=${id} plainText=[$preview] author_id=$author_id author=${author?.name}")
        val result = shouldBlock(plain) || shouldBlock(
            author_id.takeIf { it != 0L } ?: (author?.id ?: -1),
            author?.name,
            author?.nameShow,
        )
        Log.d(TAG, "<< SubPostList.shouldBlock() -> $result")
        return result
    }

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean {
        val contentStr = content.orEmpty()
        val preview = if (contentStr.length > 100) contentStr.take(100) + "..." else contentStr
        Log.d(TAG, ">> MessageInfoBean.shouldBlock() content=[$preview] replyer=${replyer?.name}")
        val result = shouldBlock(contentStr) || shouldBlock(
            this.replyer?.id?.toLongOrNull() ?: -1,
            this.replyer?.name,
            this.replyer?.nameShow,
        )
        Log.d(TAG, "<< MessageInfoBean.shouldBlock() -> $result")
        return result
    }
}
