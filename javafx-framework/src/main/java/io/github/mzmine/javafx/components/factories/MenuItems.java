/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.javafx.components.factories;

import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import org.jetbrains.annotations.NotNull;

public class MenuItems {


  public static MenuItem create(String title, EventHandler<ActionEvent> eventHandler) {
    var item = new MenuItem(title);
    item.setOnAction(eventHandler);
    return item;
  }

  public static MenuItem create(String title, Runnable run) {
    var item = new MenuItem(title);
    item.setOnAction(e -> run.run());
    return item;
  }

  public static MenuItem create(final String title,
      final ObjectProperty<EventHandler<ActionEvent>> property) {
    var item = new MenuItem(title);
    item.setOnAction(e -> {
      var run = property.get();
      if (run != null) {
        run.handle(e);
      }
    });
    return item;
  }

  /**
   * Menu item that performs action on selected ListView item. This item is NotNull
   *
   * @param listView the parent list view
   * @param title    menu title
   * @param consumer consumer of NotNull item
   * @param <T>      the item class
   * @return a menu item
   */
  public static <T> MenuItem onSelectedItem(final @NotNull ListView<T> listView, final String title,
      final Consumer<@NotNull T> consumer) {
    return create(title, () -> {
      T selected = listView.getSelectionModel().getSelectedItem();
      if (selected == null) {
        return;
      }
      consumer.accept(selected);
    });
  }
}
