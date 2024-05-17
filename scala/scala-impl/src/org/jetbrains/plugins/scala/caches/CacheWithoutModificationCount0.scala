package org.jetbrains.plugins.scala.caches

import org.jetbrains.plugins.scala.caches.stats.Tracer

import java.util.concurrent.atomic.AtomicReference

// TODO CacheWithoutModificationCount0[R] == CacheWithoutModificationCountN[Unit, R]
private class CacheWithoutModificationCount0[R](id: String, name: String, wrapper: ValueWrapper[R], cleanupScheduler: CleanupScheduler) {
  // Code generated by @CachedWithoutModificationCount macro annotation, method without parameters

  private[this] val reference: AtomicReference[wrapper.Reference] = new AtomicReference()

  cleanupScheduler.subscribe { () =>
    reference.set(null.asInstanceOf[wrapper.Reference])
  }

  def apply(f: => R): R = {
    val tracer = Tracer(id, name)
    tracer.invocation()
    val ref = reference.get()
    val resultFromCache = if (ref == null) null.asInstanceOf[R] else wrapper.unwrap(ref)
    if (resultFromCache == null) {
      tracer.calculationStart()
      val computedValue = try {
        f
      } finally {
        tracer.calculationEnd()
      }
      assert(computedValue != null, "Cached function should never return null")
      reference.set(wrapper.wrap(computedValue))
      computedValue
    } else {
      resultFromCache
    }
  }

/*
@CachedWithoutModificationCount(ValueWrapper.SofterReference, cleanupScheduler)
def foo(): String = "Foo"

new _root_.scala.volatile();
private var org$example$Example$foo: _root_.com.intellij.util.SofterReference[String] = null.asInstanceOf[_root_.com.intellij.util.SofterReference[String]];
cleanupScheduler.asInstanceOf[_root_.org.jetbrains.plugins.scala.caches.CleanupScheduler].subscribe((() => org$example$Example$foo = null));
def foo(): String = {
  val org$example$Example$foo$$tracer = _root_.org.jetbrains.plugins.scala.caches.stats.Tracer("org$example$Example$foo$cacheKey", "Example.foo");
  org$example$Example$foo$$tracer.invocation();
  val resultFromCache = if (org$example$Example$foo.$eq$eq(null))
    null
  else
    org$example$Example$foo.get();
  if (resultFromCache.$eq$eq(null))
    {
      org$example$Example$foo$$tracer.calculationStart();
      val org$example$Example$foo$computedValue = try {
        "Foo"
      } finally org$example$Example$foo$$tracer.calculationEnd();
      assert(org$example$Example$foo$computedValue.$bang$eq(null), "Cached function should never return null");
      org$example$Example$foo = new _root_.com.intellij.util.SofterReference(org$example$Example$foo$computedValue);
      org$example$Example$foo$computedValue
    }
  else
    resultFromCache
};
()
*/
}
