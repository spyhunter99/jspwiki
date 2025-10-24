/*
 * Copyright (C) 2014 David Vittor http://digitalspider.com.au
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
 */
package org.apache.jspwiki.plugins.emoji;


import java.util.Collection;
import org.junit.jupiter.api.Assertions;

public class EmojiFilterTest {

    public void testEmojiSyntax() {
        String content = "This is a :::bowtie::: emoji with a :::smiley::: face, but not inside <pre> :::noformat::: \n tags </pre>\n"
                + "However :::outsidepre::: has another <pre>:::insidepre:::</pre>";

        Collection<String> htmlStrings = EmojiFilter.findByRegex(content, EmojiFilter.REGEX_EMOJI);
//        System.out.println("htmlStrings="+htmlStrings);
        Assertions.assertEquals(3, htmlStrings.size());
        Assertions.assertTrue(htmlStrings.contains(":::bowtie:::"));
        Assertions.assertTrue(htmlStrings.contains(":::smiley:::"));
        Assertions.assertFalse(htmlStrings.contains(":::noformat:::"));
        Assertions.assertTrue(htmlStrings.contains(":::outsidepre:::"));
        Assertions.assertFalse(htmlStrings.contains(":::insidepre:::"));

        content = EmojiFilter.replaceEmoji(content, htmlStrings);
        String expectedContent
                = "This is a <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/bowtie.png' height=20 weigth=20 /></span> "
                + "emoji with a "
                + "<span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/smiley.png' height=20 weigth=20 /></span> "
                + "face, but not inside <pre> :::noformat::: \n"
                + " tags </pre>\n"
                + "However <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/outsidepre.png' height=20 weigth=20 /></span> has another <pre>:::insidepre:::</pre>";
        System.out.println("content=" + content);
        Assertions.assertEquals(expectedContent, content);
    }

    public void testEmojiSyntaxSecondTime() {
        String content
                = "This is a <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/bowtie.png' height=20 weigth=20 /></span> "
                + "emoji with a :::smiley::: face, but not inside <pre> :::noformat::: \n"
                + " tags </pre>\n"
                + "However <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/outsidepre.png' height=20 weigth=20 /></span> has another <pre>:::insidepre:::</pre>";

        Collection<String> htmlStrings = EmojiFilter.findByRegex(content, EmojiFilter.REGEX_EMOJI);
//        System.out.println("htmlStrings="+htmlStrings);
        Assertions.assertEquals(1, htmlStrings.size());
        Assertions.assertFalse(htmlStrings.contains(":::bowtie:::"));
        Assertions.assertTrue(htmlStrings.contains(":::smiley:::"));
        Assertions.assertFalse(htmlStrings.contains(":::noformat:::"));
        Assertions.assertFalse(htmlStrings.contains(":::outsidepre:::"));
        Assertions.assertFalse(htmlStrings.contains(":::insidepre:::"));

        content = EmojiFilter.replaceEmoji(content, htmlStrings);
        String expectedContent
                = "This is a <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/bowtie.png' height=20 weigth=20 /></span> "
                + "emoji with a "
                + "<span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/smiley.png' height=20 weigth=20 /></span> "
                + "face, but not inside <pre> :::noformat::: \n"
                + " tags </pre>\n"
                + "However <span class='emoji'><img src='http://www.emoji-cheat-sheet.com/graphics/emojis/outsidepre.png' height=20 weigth=20 /></span> has another <pre>:::insidepre:::</pre>";
        Assertions.assertEquals(expectedContent, content);
    }
}
