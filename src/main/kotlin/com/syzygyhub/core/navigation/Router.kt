package com.syzygyhub.core.navigation

/**
 * A navigable route with a [path] and optional [parameters].
 */
interface Route {
    /** The path component of the route, e.g. "/home" or "/user/42". */
    val path: String

    /** Key-value parameters extracted from the route. */
    val parameters: Map<String, String> get() = emptyMap()
}

/**
 * A simple data-class implementation of [Route].
 */
data class SimpleRoute(
    override val path: String,
    override val parameters: Map<String, String> = emptyMap(),
) : Route

/**
 * Guard that can prevent navigation to a route.
 */
interface RouteGuard {
    /** Returns true if navigation to [to] should be allowed. */
    fun canNavigate(to: Route): Boolean
}

/**
 * Manages a navigation stack and applies [RouteGuard] checks before navigation.
 */
class Router {
    private val stack = mutableListOf<Route>()
    private val guards = mutableListOf<RouteGuard>()

    /** The route currently at the top of the stack, or null if empty. */
    val currentRoute: Route? get() = stack.lastOrNull()

    /** The current depth of the navigation stack. */
    val stackDepth: Int get() = stack.size

    /**
     * Pushes a [route] onto the stack if all guards allow it.
     * @return true if the route was pushed.
     */
    fun push(route: Route): Boolean {
        if (!guardsAllow(route)) return false
        stack.add(route)
        return true
    }

    /**
     * Pops the top route from the stack.
     * @return the removed route, or null if the stack was empty.
     */
    fun pop(): Route? = if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else null

    /**
     * Pops all routes except the root (first) entry.
     */
    fun popToRoot() {
        if (stack.size > 1) {
            val root = stack.first()
            stack.clear()
            stack.add(root)
        }
    }

    /**
     * Replaces the top route with [route] if all guards allow it.
     * @return true if replacement succeeded.
     */
    fun replace(route: Route): Boolean {
        if (!guardsAllow(route)) return false
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(route)
        return true
    }

    /** Adds a [guard] that is consulted before every navigation. */
    fun addGuard(guard: RouteGuard) {
        guards.add(guard)
    }

    private fun guardsAllow(route: Route): Boolean = guards.all { it.canNavigate(route) }
}

/**
 * Parses deep link URLs into [Route] instances based on registered patterns.
 *
 * Patterns use `{name}` placeholders for path parameters,
 * e.g. `"/user/{id}/profile"`.
 */
class DeepLinkParser {
    private data class PatternEntry(
        val regex: Regex,
        val paramNames: List<String>,
        val factory: (Map<String, String>) -> Route,
    )

    private val patterns = mutableListOf<PatternEntry>()

    /**
     * Registers a URL [pattern] with a [routeFactory] that constructs a [Route]
     * from extracted parameters.
     */
    fun register(
        pattern: String,
        routeFactory: (Map<String, String>) -> Route,
    ) {
        val paramNames = mutableListOf<String>()
        val regexPattern =
            pattern.split("/").joinToString("/") { segment ->
                val match = Regex("\\{(\\w+)}").matchEntire(segment)
                if (match != null) {
                    paramNames.add(match.groupValues[1])
                    "([^/]+)"
                } else {
                    Regex.escape(segment)
                }
            }
        patterns.add(PatternEntry(Regex("^$regexPattern$"), paramNames, routeFactory))
    }

    /**
     * Parses a [url] against registered patterns and returns the first matching [Route],
     * or null if no pattern matches.
     */
    fun parse(url: String): Route? {
        // Strip scheme and host to get path
        val path =
            if (url.contains("://")) {
                val afterScheme = url.substringAfter("://")
                val pathStart = afterScheme.indexOf('/')
                if (pathStart >= 0) afterScheme.substring(pathStart) else "/"
            } else {
                url
            }

        for (entry in patterns) {
            val matchResult = entry.regex.matchEntire(path) ?: continue
            val params =
                entry.paramNames.zip(
                    matchResult.groupValues.drop(1),
                ).toMap()
            return entry.factory(params)
        }
        return null
    }
}
