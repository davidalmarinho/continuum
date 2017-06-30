/*
 * Copyright 2014-2017 Gil Mendes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.continuum.rendering;

import org.continuum.main.Continuum;
import javolution.util.FastMap;
import org.newdawn.slick.AngelCodeFont;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.ResourceLoader;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Provides global access to fonts.
 */
public class FontManager {

    private final FastMap<String, AngelCodeFont> _fonts = new FastMap<String, AngelCodeFont>();
    private static FontManager _instance = null;

    /**
     * Returns (and creates – if necessary) the static instance
     * of this helper class.
     *
     * @return The instance
     */
    public static FontManager getInstance() {
        if (_instance == null) {
            _instance = new FontManager();
        }

        return _instance;
    }

    private FontManager() {
        initFonts();
    }

    private void initFonts() {
        try {
            _fonts.put("default", new AngelCodeFont("Font", ResourceLoader.getResource("org/continuum/data/fonts/default.fnt").openStream(), ResourceLoader.getResource("org/continuum/data/fonts/default_0.png").openStream()));
        } catch (SlickException e) {
            Continuum.getInstance().getLogger().log(Level.SEVERE, "Couldn't load fonts. Sorry. " + e.toString(), e);
        } catch (IOException e) {
            Continuum.getInstance().getLogger().log(Level.SEVERE, "Couldn't load fonts. Sorry. " + e.toString(), e);
        }
    }

    public AngelCodeFont getFont(String s) {
        return _fonts.get(s);
    }
}
