/*
 * MediaModuleTest.java
 * JUnit based test
 *
 * Created on March 29, 2006, 11:49 PM
 */

package org.rometools.feed.module.mediarss;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.ElementNameQualifier;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.rometools.feed.module.AbstractTestCase;
import org.rometools.feed.module.mediarss.types.MediaContent;
import org.rometools.feed.module.mediarss.types.Rating;
import org.rometools.feed.module.mediarss.types.Thumbnail;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * 
 * @author cooper
 */
public class MediaModuleTest extends AbstractTestCase {

    /**
     * @param testName id of test
     */
    public MediaModuleTest(final String testName) {
        super(testName);
    }

    /**
     * @return actual test suite
     */
    public static Test suite() {
        return new TestSuite(MediaModuleTest.class);
    }

    /**
     * @throws Exception if file not found or not accessible
     */
    public void testGoogleVideo() throws Exception {
        final SyndFeed feed = getSyndFeed("data/YouTube-MostPopular.rss");
        for (final Object element : feed.getEntries()) {
            final SyndEntry entry = (SyndEntry) element;
            final MediaEntryModule m = (MediaEntryModule) entry.getModule(MediaModule.URI);
            assertNotNull(m);
        }
    }

    /**
     * @throws Exception if file not found or not accessible
     */
    public void testParse() throws Exception {
        final File test = new File(getTestFile("xml"));
        final File[] files = test.listFiles();
        for (int j = 0; j < files.length; j++) {
            if (!files[j].getName().endsWith(".xml")) {
                continue;
            }
            compareFeedFiles(files[j], new File("target/" + j + ".xml"));
        }
    }

    /**
     * @param expected original file
     * @param generated file for output
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    private void compareFeedFiles(final File expected, final File generated) throws IOException, FeedException {
        final SyndFeed feed = getSyndFeed(expected);
        final List<SyndEntry> entries = feed.getEntries();
        final SyndFeedOutput output = new SyndFeedOutput();
        output.output(feed, generated);
        final SyndFeed feed2 = getSyndFeed(generated);
        for (int i = 0; i < entries.size(); i++) {
            BufferedWriter b = new BufferedWriter(new FileWriter(generated.getAbsolutePath() + ".a.txt"));
            b.write("" + entries.get(i).getModule(MediaModule.URI));
            b.close();
            b = new BufferedWriter(new FileWriter(generated.getAbsolutePath() + ".b.txt"));
            b.write("" + feed2.getEntries().get(i).getModule(MediaModule.URI));
            b.close();
            assertEquals(entries.get(i).getModule(MediaModule.URI), feed2.getEntries().get(i).getModule(MediaModule.URI));
        }
    }

    /**
     * tests parsing thubnails with empty dimensions (https://github.com/rometools/rome-modules/issues/7).
     * 
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    public void testParseThumbnailWithEmptyDimensions() throws FeedException, IOException {
        final MediaEntryModule module = getFirstModuleFromFile("org/rometools/feed/module/mediarss/issue-07.xml");
        final Thumbnail[] thumbnails = module.getMetadata().getThumbnail();

        assertThat(thumbnails, is(notNullValue()));
    }

    /**
     * tests parsing a decimal duration (https://github.com/rometools/rome-modules/issues/8).
     * 
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    public void testParseDecimalDuration() throws FeedException, IOException {
        final MediaEntryModule module = getFirstModuleFromFile("org/rometools/feed/module/mediarss/issue-08.xml");
        final Thumbnail[] thumbnails = module.getMetadata().getThumbnail();

        assertThat(thumbnails, is(notNullValue()));
    }

    /**
     * tests parsing rating without scheme (https://github.com/rometools/rome-modules/issues/12).
     * 
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    public void testParseRatingWithoutScheme() throws FeedException, IOException {
        final MediaEntryModule module = getFirstModuleFromFile("org/rometools/feed/module/mediarss/issue-12.xml");
        final Rating[] ratings = module.getMetadata().getRatings();

        assertThat(ratings, is(notNullValue()));
    }

    /**
     * test url with whitespace in media element (https://github.com/rometools/rome-modules/issues/20).
     * 
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    public void testParseMediaContentContainingURLWithSpaces() throws FeedException, IOException {
        final MediaEntryModule module = getFirstModuleFromFile("org/rometools/feed/module/mediarss/issue-20.xml");
        assertNotNull("missing media entry module", module);
        final MediaContent[] mediaContents = module.getMediaContents();
        assertNotNull("missing media:content", mediaContents);
        assertEquals("wrong count of media:content", 1, mediaContents.length);
        final MediaContent mediaContent = mediaContents[0];
        assertEquals("http://www.foo.com/path/containing+spaces/trailer.mov", mediaContent.getReference().toString());
    }

    /**
     * tests parsing of MediaRSS 1.5 elements (https://github.com/rometools/rome-modules/issues/15).
     * 
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    public void testParseMediaRss15() throws FeedException, IOException {
        final MediaEntryModule module = getFirstModuleFromFile("org/rometools/feed/module/mediarss/issue-15.xml");
        assertNotNull("missing media entry module", module);
        assertNotNull("missing metadata element", module.getMetadata());
        assertNotNull("missing community", module.getMetadata().getCommunity());
        assertNotNull("missing community starRating", module.getMetadata().getCommunity().getStarRating());
        assertNotNull("missing community statistics", module.getMetadata().getCommunity().getStatistics());
        assertNotNull("missing community tags", module.getMetadata().getCommunity().getTags());
        assertEquals("missing comments", 2, module.getMetadata().getComments().length);
        assertEquals("missing responses", 2, module.getMetadata().getResponses().length);
        assertEquals("missing backLinks", 2, module.getMetadata().getBackLinks().length);
        assertNotNull("missing state",  module.getMetadata().getStatus());
        assertEquals("missing price", 1, module.getMetadata().getPrices().length);
        assertNotNull("missing embed",  module.getMetadata().getEmbed());
        assertEquals("missing embed params", 5, module.getMetadata().getEmbed().getParams().length);
        assertEquals("missing license", 1, module.getMetadata().getLicenses().length);
        assertEquals("missing subTitle", 1, module.getMetadata().getSubTitles().length);
        assertEquals("missing peerLinks", 1, module.getMetadata().getPeerLinks().length);
        assertEquals("missing subTitle", 1, module.getMetadata().getSubTitles().length);
        assertEquals("missing location", 1, module.getMetadata().getLocations().length);
        assertNotNull("missing rights", module.getMetadata().getRights());
        assertEquals("missing scenes", 1, module.getMetadata().getScenes().length);
    }

    /**
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     * @throws SAXException if xml parsing fails
     */
    public void testMediaRss15Generator() throws IOException, FeedException, SAXException {
        final SyndFeed feed = getSyndFeed("org/rometools/feed/module/mediarss/issue-15.xml");
        final SyndFeedOutput output = new SyndFeedOutput();
        output.output(feed, new File("target/issue-15.xml"));
        XMLUnit.setIgnoreWhitespace(true);
        // text content and attribute values on some elements are reformatted, so they should be ignored
        final List<String> tagsToIgnoreTextContent = Arrays.asList("tags", "sceneStartTime", "sceneEndTime");
        final List<String> attributaVluesToIgnore = Arrays.asList("bitrate", "end", "start");
        final Diff myDiff = new Diff(new FileReader(new File(getTestFile("org/rometools/feed/module/mediarss/issue-15.xml"))),
                new FileReader(new File("target/issue-15.xml")));
        myDiff.overrideElementQualifier(new ElementNameQualifier());
        myDiff.overrideDifferenceListener(new DifferenceListener() {
            
            @Override
            public void skippedComparison(final Node control, final Node test) {
            }
            
            @Override
            public int differenceFound(final Difference difference) {
                if (difference.getId() == DifferenceConstants.TEXT_VALUE_ID) {
                    final Node diffNode = difference.getControlNodeDetail().getNode().getParentNode();
                    if (MediaModule.URI.equals(diffNode.getNamespaceURI()) && tagsToIgnoreTextContent.contains(diffNode.getLocalName())) {
                        return DifferenceListener.RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
                    }
                } else if (difference.getId() == DifferenceConstants.ATTR_VALUE_ID) {
                    final Node diffNode = difference.getControlNodeDetail().getNode();
                    if (attributaVluesToIgnore.contains(diffNode.getLocalName())) {
                        return DifferenceListener.RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
                    }
                }
                return DifferenceListener.RETURN_ACCEPT_DIFFERENCE;
            }
        });
        XMLAssert.assertXMLEqual(myDiff, true);
    }

    /**
     * @param filePath relative path to file
     * @return MediaEntryModule of first feed item
     * @throws IOException if file access failed
     * @throws FeedException if parsing feed failed
     */
    private MediaEntryModule getFirstModuleFromFile(final String filePath) throws IOException, FeedException {
        final SyndFeed feed = getSyndFeed(filePath);
        final SyndEntry entry = feed.getEntries().get(0);
        return (MediaEntryModule) entry.getModule(MediaEntryModule.URI);
    }

    /**
     * @param file to read
     * @return SyndFeed from file
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    private SyndFeed getSyndFeed(final File file) throws IOException, FeedException {
        return new SyndFeedInput().build(file);
    }

    /**
     * @param filePath relative path to file
     * @return SyndFeed from file
     * @throws IOException if file not found or not accessible
     * @throws FeedException when the feed can't be parsed
     */
    private SyndFeed getSyndFeed(final String filePath) throws IOException, FeedException {
        final String fullPath = getTestFile(filePath);
        final File file = new File(fullPath);
        return getSyndFeed(file);
    }

}
