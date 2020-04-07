/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
@Deploy("org.nuxeo.apidoc.repo:apidoc-plugin-test-contrib.xml")
public class TestPlugin {

    @Inject
    protected SnapshotManager snapshotManager;

    @Test
    public void testRegistration() {
        Plugin<?> foo = snapshotManager.getPlugin("foo");
        assertNull(foo);

        Plugin<?> p = snapshotManager.getPlugin("testPlugin");
        assertNotNull(p);
    }

    @Test
    public void testPlugins() {
        List<Plugin<?>> plugins = snapshotManager.getPlugins();
        assertNotNull(plugins);
        assertEquals(1, plugins.size());
    }

    @Test
    public void testPlugin() {
        Plugin<?> p = snapshotManager.getPlugin("testPlugin");
        assertNotNull(p);
        assertEquals("testPlugin", p.getId());
        assertEquals("myType", p.getViewType());
        assertEquals("My snapshot plugin", p.getLabel());
        assertEquals("listItems", p.getHomeView());
        assertEquals("myStyleClass", p.getStyleClass());
        assertFalse(p.isHidden());
    }

}
