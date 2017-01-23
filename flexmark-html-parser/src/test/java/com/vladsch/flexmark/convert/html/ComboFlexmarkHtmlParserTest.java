package com.vladsch.flexmark.convert.html;

import com.vladsch.flexmark.IParse;
import com.vladsch.flexmark.IRender;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.spec.SpecExample;
import com.vladsch.flexmark.spec.SpecReader;
import com.vladsch.flexmark.test.ComboSpecTestCase;
import com.vladsch.flexmark.test.DumpSpecReader;
import com.vladsch.flexmark.test.FullSpecTestCase;
import com.vladsch.flexmark.test.RenderingTestCase;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.junit.AssumptionViolatedException;
import org.junit.ComparisonFailure;
import org.junit.runners.Parameterized;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ComboFlexmarkHtmlParserTest extends ComboSpecTestCase {
    private static final String SPEC_RESOURCE = "/flexmark_html_parser_spec.md";
    private static final DataHolder OPTIONS = new MutableDataSet()
            .set(HtmlRenderer.INDENT_SIZE, 2)
            //.set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
            //.set(Parser.EXTENSIONS, Collections.singleton(FlexmarkHtmlParser.create())
            ;

    private static final Map<String, DataHolder> optionsMap = new HashMap<>();
    static {
        optionsMap.put("src-pos", new MutableDataSet().set(HtmlRenderer.SOURCE_POSITION_ATTRIBUTE, "md-pos"));
        // optionsMap.put("option1", new MutableDataSet().set(FlexmarkHtmlParserExtension.FLEXMARK_HTML_PARSER_OPTION1, true));
    }

    private static final IParse PARSER = new HtmlParser(OPTIONS);

    private static final IRender RENDERER = new HtmlRootNodeRenderer(OPTIONS);

    private static DataHolder optionsSet(String optionSet) {
        return optionsMap.get(optionSet);
    }

    public ComboFlexmarkHtmlParserTest(SpecExample example) {
        super(example);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> data() {
        List<SpecExample> examples = SpecReader.readExamples(SPEC_RESOURCE);
        List<Object[]> data = new ArrayList<>();

        // NULL example runs full spec test
        data.add(new Object[] { SpecExample.NULL });

        for (SpecExample example : examples) {
            // flip source and html
            data.add(new Object[] { example });
        }
        return data;
    }

    @Override
    public DataHolder options(String optionSet) {
        return optionsSet(optionSet);
    }

    @Override
    public String getSpecResourceName() {
        return SPEC_RESOURCE;
    }

    @Override
    public IParse parser() {
        return PARSER;
    }

    @Override
    public IRender renderer() {
        return RENDERER;
    }

    private static class HtmlSpecReader extends DumpSpecReader {
        public HtmlSpecReader(final InputStream stream, final FullSpecTestCase testCase) {
            super(stream, testCase);
        }

        @Override
        protected void addSpecExample(SpecExample example) {
            DataHolder options;
            boolean ignoredCase = false;
            try {
                options = testCase.getOptions(example, example.getOptionsSet());
            } catch (AssumptionViolatedException ignored) {
                ignoredCase = true;
                options = null;
            }

            if (options != null && options.get(FAIL)) {
                ignoredCase = true;
            }

            String parseSource = example.getHtml();
            if (options != null && options.get(RenderingTestCase.NO_FILE_EOL)) {
                parseSource = trimTrailingEOL(parseSource);
            }

            Node node = testCase.parser().withOptions(options).parse(parseSource);
            String source = !ignoredCase && testCase.useActualHtml() ? testCase.renderer().withOptions(options).render(node) : example.getSource();
            String html = example.getHtml();
            String ast = example.getAst() == null ? null : (!ignoredCase ? testCase.ast(node) : example.getAst());

            // include source so that diff can be used to update spec
            addSpecExample(sb, source, html, ast, example.getOptionsSet(), testCase.includeExampleCoords(), example.getSection(), example.getExampleNumber());
        }
    }

    @Override
    public SpecReader create(InputStream inputStream) {
        dumpSpecReader = new HtmlSpecReader(inputStream, this);
        return dumpSpecReader;
    }

    // reverse source and html
    @Override
    protected void assertRendering(String source, String expectedHtml, String optionsSet) {
        DataHolder options = optionsSet == null ? null : getOptions(example(), optionsSet);
        String parseSource = expectedHtml;

        if (options != null && options.get(NO_FILE_EOL)) {
            parseSource = DumpSpecReader.trimTrailingEOL(parseSource);
        }

        Node node = parser().withOptions(options).parse(parseSource);
        String renderedResult = renderer().withOptions(options).render(node);
        String expectedResult = source;

        actualSource(renderedResult, optionsSet);

        boolean useActualHtml = useActualHtml();

        // include source for better assertion errors
        String expected;
        String actual;
        if (example() != null && example().getSection() != null) {
            StringBuilder outExpected = new StringBuilder();
            DumpSpecReader.addSpecExample(outExpected, expectedResult, expectedHtml, "", optionsSet, true, example().getSection(), example().getExampleNumber());
            expected = outExpected.toString();

            StringBuilder outActual = new StringBuilder();
            DumpSpecReader.addSpecExample(outActual, useActualHtml ? renderedResult : expectedResult, expectedHtml, "", optionsSet, true, example().getSection(), example().getExampleNumber());
            actual = outActual.toString();
        } else {
            expected = DumpSpecReader.addSpecExample(expectedResult, expectedHtml, "", optionsSet);
            actual = DumpSpecReader.addSpecExample(useActualHtml ? renderedResult : expectedResult, expectedHtml, "", optionsSet);
        }

        specExample(expected, actual, optionsSet);
        if (options != null && options.get(FAIL)) {
            thrown.expect(ComparisonFailure.class);
        }
        assertEquals(expected, actual);
    }
}