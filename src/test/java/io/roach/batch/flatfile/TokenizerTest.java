package io.roach.batch.flatfile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.batch.item.file.transform.RegexLineTokenizer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;

public class TokenizerTest {

    @Test
    public void foo() throws Exception {
        Map<String, LineTokenizer> tokenizers = new HashMap<>(3);
//        tokenizers.put("USER*", userTokenizer());
//        tokenizers.put("LINEA*", lineATokenizer());
//        tokenizers.put("LINEB*", lineBTokenizer());

        Map<String, FieldSetMapper<String[]>> mappers = new HashMap<>(2);
//        mappers.put("USER*", userFieldSetMapper());
//        mappers.put("LINE*", lineFieldSetMapper());

        PatternMatchingCompositeLineMapper<String[]> lineMapper =
                new PatternMatchingCompositeLineMapper<>();
        lineMapper.setTokenizers(tokenizers);
        lineMapper.setFieldSetMappers(mappers);

        RegexLineTokenizer regexLineTokenizer = new RegexLineTokenizer();
//        regexLineTokenizer.setRegex("");
        regexLineTokenizer.setNames("");

        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setNames(";");
        delimitedLineTokenizer.setIncludedFields(1, 2, 3);
        delimitedLineTokenizer.setQuoteCharacter('"');
        delimitedLineTokenizer.setStrict(true);

        FixedLengthTokenizer fixedLengthTokenizer = new FixedLengthTokenizer();
        fixedLengthTokenizer.setNames("ISIN", "Quantity", "Price", "Customer");
        fixedLengthTokenizer.setColumns(new Range(1, 12),
                new Range(13, 15),
                new Range(16, 20),
                new Range(21, 29)
        );

        DefaultLineMapper<String[]> lineMapper1 = new DefaultLineMapper<>();
        lineMapper1.setLineTokenizer(fixedLengthTokenizer);
        lineMapper1.afterPropertiesSet();

        FlatFileItemReader<String[]> reader = new FlatFileItemReaderBuilder<String[]>()
                .comments("--", "#")
                .linesToSkip(2)
                .maxItemCount(11)
                .encoding("UTF-8")
                .strict(true)
                .saveState(false)
                .resource(new ClassPathResource("samples/test_sm.txt"))
                .lineTokenizer(fixedLengthTokenizer)
                .lineMapper(lineMapper1)
                .fieldSetMapper(new FieldSetMapper<String[]>() {
                    @Override
                    public String[] mapFieldSet(FieldSet fieldSet) throws BindException {
                        return fieldSet.getValues();
                    }
                })
                .build();

        reader.open(new ExecutionContext());

        ItemWriter<String> itemWriter = items -> {
            System.out.println(StringUtils.collectionToCommaDelimitedString(items));
        };

        for (; ; ) {
            String[] tokens = reader.read();
            if (tokens != null) {
                itemWriter.write(Arrays.asList(tokens));
            } else {
                break;
            }
        }
    }
}
