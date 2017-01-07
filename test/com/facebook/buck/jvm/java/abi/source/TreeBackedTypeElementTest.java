/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java.abi.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.testutil.CompilerTreeApiParameterized;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

@RunWith(CompilerTreeApiParameterized.class)
public class TreeBackedTypeElementTest extends CompilerTreeApiParameterizedTest {

  @Test
  public void testGetSimpleName() throws IOException {
    compile("public class Foo {}");
    TypeElement element = elements.getTypeElement("Foo");

    assertNameEquals("Foo", element.getSimpleName());
  }

  @Test
  public void testGetQualifiedNameUnnamedPackage() throws IOException {
    compile("public class Foo {}");

    TypeElement element = elements.getTypeElement("Foo");

    assertNameEquals("Foo", element.getQualifiedName());
  }

  @Test
  public void testGetQualifiedNameNamedPackage() throws IOException {
    compile(Joiner.on('\n').join(
        "package com.facebook.buck;",
        "public class Foo {}"));

    TypeElement element = elements.getTypeElement("com.facebook.buck.Foo");

    assertNameEquals("com.facebook.buck.Foo", element.getQualifiedName());
  }

  @Test
  public void testToString() throws IOException {
    compile(Joiner.on('\n').join(
        "package com.facebook.buck;",
        "public class Foo<T> {}"));

    TypeElement element = elements.getTypeElement("com.facebook.buck.Foo");

    assertEquals("com.facebook.buck.Foo", element.toString());
  }

  @Test
  public void testGetQualifiedNameInnerClass() throws IOException {
    compile(Joiner.on('\n').join(
        "package com.facebook.buck;",
        "public class Foo {",
        "  public class Bar { }",
        "}"));

    TypeElement element = elements.getTypeElement("com.facebook.buck.Foo.Bar");

    assertNameEquals("com.facebook.buck.Foo.Bar", element.getQualifiedName());
  }

  @Test
  public void testGetQualifiedNameEnumAnonymousMember() throws IOException {
    compile(Joiner.on('\n').join(
        "public enum Foo {",
        "  BAR,",
        "  BAZ() {",
        "  }",
        "}"
    ));

    // Expect no crash. Enum anonymous members don't have qualified names, but at one point during
    // development we would crash trying to make one for them.
  }

  @Test
  public void testAsType() throws IOException {
    compile("class Foo { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    TypeMirror fooTypeMirror = fooElement.asType();

    assertEquals(TypeKind.DECLARED, fooTypeMirror.getKind());
    DeclaredType fooDeclaredType = (DeclaredType) fooTypeMirror;
    assertSame(fooElement, fooDeclaredType.asElement());
    assertEquals(0, fooDeclaredType.getTypeArguments().size());

    TypeMirror enclosingType = fooDeclaredType.getEnclosingType();
    assertEquals(TypeKind.NONE, enclosingType.getKind());
    assertTrue(enclosingType instanceof NoType);
  }

  @Test
  public void testGetSuperclassNoSuperclassIsObject() throws IOException {
    compile("class Foo { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    DeclaredType superclass = (DeclaredType) fooElement.getSuperclass();
    TypeElement objectElement = elements.getTypeElement("java.lang.Object");

    assertSame(objectElement, superclass.asElement());
  }

  @Test
  public void testGetSuperclassObjectSuperclassIsObject() throws IOException {
    compile("class Foo extends java.lang.Object { }");

    TypeElement fooElement = elements.getTypeElement("Foo");
    DeclaredType superclass = (DeclaredType) fooElement.getSuperclass();
    TypeElement objectElement = elements.getTypeElement("java.lang.Object");

    assertSame(objectElement, superclass.asElement());
  }

  @Test
  public void testGetSuperclassOfInterfaceIsNoneType() throws IOException {
    compile("interface Foo extends java.lang.Runnable { }");

    TypeElement fooElement = elements.getTypeElement("Foo");

    assertSame(TypeKind.NONE, fooElement.getSuperclass().getKind());
  }

  @Test
  public void testGetSuperclassOtherSuperclass() throws IOException {
    compile(ImmutableMap.of(
        "Foo.java",
        Joiner.on('\n').join(
            "package com.facebook.foo;",
            "public class Foo extends com.facebook.bar.Bar { }"
        ),
        "Bar.java",
        Joiner.on('\n').join(
            "package com.facebook.bar;",
            "public class Bar { }"
        )
    ));

    TypeElement fooElement = elements.getTypeElement("com.facebook.foo.Foo");
    TypeElement barElement = elements.getTypeElement("com.facebook.bar.Bar");

    DeclaredType superclass = (DeclaredType) fooElement.getSuperclass();
    assertSame(barElement, superclass.asElement());
  }

  private void assertNameEquals(String expected, Name actual) {
    assertEquals(elements.getName(expected), actual);
  }
}
