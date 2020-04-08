package xmi;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import utils.*;

import java.nio.file.Path;
import java.util.List;

import static utils.ThrowingBiConsumer.throwingBiConsumerWrapper;


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

    private EntityAligner(CasDoc reference, CasDoc external) throws IllegalArgumentException {
        this.reference = reference;
        this.external = external;
        this.aligner = TokenAligner.create(external.getTokens(), reference.getTokens(), MAX_TOKEN_LOOK_AHEAD);
    }

    public static EntityAligner create(String reference, String entitiesFile) throws AbnormalProcessException {
        CasDoc ref = CasDoc.create();
        ref.read(reference);
        CasDoc external = CasDoc.create();
        external.read(entitiesFile);
        EntityAligner aligner = new EntityAligner(ref, external);
        return aligner;
    }

    /**
     * Maps external token indices to reference character offsets,
     * and adds entities from external document to reference document.
     */
    public void run() throws IllegalArgumentException {
        aligner.align();
        addEntities();
    }

    /**
     * Adds entities to reference document with right offsets.
     */
    private void addEntities() throws IllegalArgumentException {
        for (NamedEntity e: external.getEntities()) {
            Span ref = aligner.getReferenceSpan(e.getBegin(), e.getEnd());
            if (ref.getBegin() == -1 || ref.getEnd() < ref.getBegin())
                throw new IllegalArgumentException("Found entity with invalid reference span " + ref.toString()
                        + "; " + e.getCoveredText() + "@ " + e.getBegin() + "-" + e.getEnd());
            reference.addEntity(ref.getBegin(), ref.getEnd(), e.getValue());
        }
    }


    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".xmi")) {
            String refFile = IO.append(dirs.get(0), fileName);
            String outFile = IO.append(dirs.get(1), fileName);

            EntityAligner entityAligner = create(refFile, file.toString());
            try {
                entityAligner.run();
            } catch (IllegalArgumentException e) {
                throw new AbnormalProcessException("Cannot align entities for file " + fileName, e);
            }
            entityAligner.write(outFile);

        }
    }

    private void write(String outFile) throws AbnormalProcessException {
        reference.write(outFile);
    }

    public boolean allAligned() {
        return reference.getEntities().size() == external.getEntities().size();
    }

    public static void main(String[] args) {
        IO.loop(args[0], args[1], args[2],
                throwingBiConsumerWrapper((x, y) -> run(x, y)));
    }

}
