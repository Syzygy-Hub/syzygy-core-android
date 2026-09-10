package com.syzygyhub.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouterTest {
    @Test
    fun `push and pop routes`() {
        val router = Router()
        router.push(SimpleRoute("/home"))
        router.push(SimpleRoute("/settings"))
        assertEquals(2, router.stackDepth)
        assertEquals("/settings", router.currentRoute?.path)
        val popped = router.pop()
        assertEquals("/settings", popped?.path)
        assertEquals("/home", router.currentRoute?.path)
    }

    @Test
    fun `popToRoot keeps only first route`() {
        val router = Router()
        router.push(SimpleRoute("/root"))
        router.push(SimpleRoute("/a"))
        router.push(SimpleRoute("/b"))
        router.popToRoot()
        assertEquals(1, router.stackDepth)
        assertEquals("/root", router.currentRoute?.path)
    }

    @Test
    fun `guard blocks navigation`() {
        val router = Router()
        router.addGuard(
            object : RouteGuard {
                override fun canNavigate(to: Route) = to.path != "/admin"
            },
        )
        assertTrue(router.push(SimpleRoute("/home")))
        assertFalse(router.push(SimpleRoute("/admin")))
        assertEquals(1, router.stackDepth)
    }

    @Test
    fun `replace swaps top of stack`() {
        val router = Router()
        router.push(SimpleRoute("/a"))
        router.replace(SimpleRoute("/b"))
        assertEquals(1, router.stackDepth)
        assertEquals("/b", router.currentRoute?.path)
    }

    @Test
    fun `deep link parser extracts parameters`() {
        val parser = DeepLinkParser()
        parser.register("/user/{id}/profile") { params ->
            SimpleRoute("/user/${params["id"]}/profile", params)
        }
        val route = parser.parse("myapp://host/user/42/profile")
        assertEquals("42", route?.parameters?.get("id"))
    }

    @Test
    fun `deep link parser returns null for no match`() {
        val parser = DeepLinkParser()
        assertNull(parser.parse("/unknown"))
    }
}
