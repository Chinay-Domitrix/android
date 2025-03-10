// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.android.tools.idea.gradle.structure.configurables.ui.treeview.legacy;

import com.intellij.openapi.util.NamedRunnable;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

@Deprecated(forRemoval = true)
abstract class TreeRunnable extends NamedRunnable {
  TreeRunnable(@NotNull String name) {
    super(name);
  }

  protected abstract void perform();

  @Override
  public final void run() {
    trace("started");
    try {
      perform();
    }
    finally {
      trace("finished");
    }
  }

  abstract static class TreeConsumer<T> extends TreeRunnable implements Consumer<T> {
    TreeConsumer(@NotNull String name) {
      super(name);
    }

    @Override
    public final void accept(T t) {
      run();
    }
  }
}
