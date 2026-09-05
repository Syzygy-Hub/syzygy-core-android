package com.syzygyhub.core.navigation

/**
 * A navigable route definition.
 */
interface Route {
    val path: String
}

/**
 * Parses deep link URLs into routes.
 */
class DeepLinkParser {
    // TODO: URL pattern matching, parameter extraction
}

/**
 * Manages a navigation stack and applies route guards.
 */
class Router {
    // TODO: stack model, push/pop/replace, route guards
}
