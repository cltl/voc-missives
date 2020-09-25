package deprecated.xmiReference;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import utils.common.AbnormalProcessException;
import utils.xmi.CasDoc;
import utils.common.IO;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;

import static utils.common.ThrowingBiConsumer.throwingBiConsumerWrapper;
@Deprecated
public class TokenWriter {
    /**
     * @param args input-dir, output-file
     **/
    public static void main(String[] args) {
        IO.loopToFile(args[0], args[1], throwingBiConsumerWrapper((x, y) -> run(x, y)));
    }

    private static void write(CasDoc doc, BufferedWriter bw) throws IOException {
        List<Token> tokens = doc.getTokens();
        ListIterator<Sentence> sentIter = doc.getSentences().listIterator();
        int eos = sentIter.next().getEnd();
        ListIterator<Token> iter = tokens.listIterator();

        while (iter.hasNext()) {
            Token t = iter.next();
            if (t.getEnd() < eos) {
                bw.write(t.getText() + " ");
            } else {
                bw.write(t.getText() + "\n");
                if (sentIter.hasNext())
                    eos = sentIter.next().getEnd();
            }
        }
    }

    private static void run(Path inputFile, BufferedWriter bw) throws AbnormalProcessException {
        String fileName = inputFile.getFileName().toString();
        if (fileName.endsWith(".entities.xmi")) {
            CasDoc doc = CasDoc.create();
            doc.read(inputFile.toString());
            try {
                write(doc, bw);
            } catch (IOException ex) {
                throw new AbnormalProcessException("Cannot write " + inputFile.toString(), ex);
            }
        }
    }
}
