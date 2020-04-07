package xmi;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.CasDoc;
import utils.IO;
import utils.Span;

import java.nio.file.Path;
import java.util.List;


/**
 * Integrates entities from an external XMI into a reference XMI.
 *
 * Entity begin and end indices are remapped to fit tokens in the reference.
 */
public class EntityAligner {

    CasDoc reference;
    CasDoc external;
    /**
     * Maps indices from external to reference character offsets.
     */
    TokenAligner aligner;
    /**
     * Numbers of tokens to overlook when text is missing from the external or reference document.
     * The selected figure was necessary for one of the tested files, where the external document missed a larger portion
     * of text.
     */
    static final int MAX_TOKEN_LOOK_AHEAD = 350;

    public static final Logger logger = LogManager.getLogger(EntityAligner.class);
    private EntityAligner(CasDoc reference, CasDoc external) {
        this.reference = reference;
        this.external = external;
        this.aligner = TokenAligner.create(external.getTokens(), reference.getTokens(), MAX_TOKEN_LOOK_AHEAD);
    }

    public static EntityAligner create(String reference, String entitiesFile) {
        CasDoc ref = CasDoc.create();
        ref.read(reference);
        CasDoc external = CasDoc.create();
        external.read(entitiesFile);
        return new EntityAligner(ref, external);
    }


    /**
     * Maps external token indices to reference character offsets,
     * and adds entities from external document to reference document.
     */
    public void run() {
        if (! aligner.isEmpty()) {
            aligner.align();
            addEntities();
        }
    }

    /**
     * Adds entities to reference document with right offsets.
     */
    private void addEntities() {
        for (NamedEntity e: external.getEntities()) {
            Span ref = aligner.getReferenceSpan(e.getBegin(), e.getEnd());
            if (ref.getBegin() == -1 || ref.getEnd() < ref.getBegin())
                throw new IllegalArgumentException("Found entity with invalid reference span " + ref.toString()
                        + "; " + e.getCoveredText() + "@ " + e.getBegin() + "-" + e.getEnd());
            reference.addEntity(ref.getBegin(), ref.getEnd(), e.getValue());
        }
    }

    private void write(String outFile) {
        reference.write(outFile);
    }

    public static void run(Path file, List<String> dirs) {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".xmi")) {
            String refFile = IO.append(dirs.get(0), fileName);
            String outFile = IO.append(dirs.get(1), fileName);

            EntityAligner entityAligner = EntityAligner.create(refFile, file.toString());
            try {
                entityAligner.run();
            } catch (IllegalArgumentException e) {
                logger.fatal("Error while aligning entities for " + fileName, e);
            }
            entityAligner.write(outFile);
        }
    }

    public boolean allAligned() {
        return reference.getEntities().size() == external.getEntities().size();
    }

    public static void main(String[] args) {
        IO.loop(args[0], args[1], args[2], (x, y) -> run(x, y));
    }

}
