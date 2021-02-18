package manIn2naf;

import missives.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.NafHandler;
import utils.xmi.CasDoc;
import xjc.naf.*;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Integrates manual entity annotations from an XMI file into
 * a reference NAF file.
 * Entities from the XMI file are mapped against the raw text of the
 * reference NAF, and then matched to overlapping tokens. The reference
 * NAF is accordingly enriched with an entities layer.
 */
public class NafXmiReader {
    NafHandler refNaf;
    CasDoc xmi;
    private final static String NAME = "man-in2naf";
    public static final Logger logger = LogManager.getLogger(NafXmiReader.class);
    EntityAligner entityAligner;
    String entityPfx;

    public NafXmiReader(String refNaf, String inputXmi) throws AbnormalProcessException {
        logger.info(inputXmi);
        this.xmi = CasDoc.create(inputXmi);
        this.refNaf = NafHandler.create(refNaf);
        this.entityPfx = this.refNaf.entityPfx();
    }

    public NafXmiReader(String refNaf, CasDoc inputXmi) throws AbnormalProcessException {
        logger.info(refNaf);
        this.xmi = inputXmi;
        this.refNaf = NafHandler.create(refNaf);
        this.entityPfx = this.refNaf.entityPfx();
    }

    public static NafXmiReader createTeiTextReader(String refNaf, String inputXmi) throws AbnormalProcessException {
        logger.info(refNaf);
        NafXmiReader nafXmiReader = new NafXmiReader(refNaf, inputXmi);
        nafXmiReader.createTeiTextEntityAligner();
        return nafXmiReader;
    }

    public static NafXmiReader createTeiTextReader(String refNaf, CasDoc inputXmi) throws AbnormalProcessException {
        logger.info(refNaf);
        NafXmiReader nafXmiReader = new NafXmiReader(refNaf, inputXmi);
        nafXmiReader.createTeiTextEntityAligner();
        return nafXmiReader;
    }


    public String getRawNafText() {
        return refNaf.getRawText();
    }

    private void createTeiTextEntityAligner() {
        entityAligner = new EntityAlignerTei(refNaf, xmi, this.entityPfx);
    }

    public EntityAligner getEntityAligner() {
        return entityAligner;
    }

    public CasDoc getXmi() {
        return xmi;
    }

    public void createEntitiesLayer(List<Entity> nafEntities) {
        refNaf.createEntitiesLayer(nafEntities, getName());
    }

    public void transferEntities() {
        List<Entity> nafEntities = entityAligner.align();
        refNaf.createEntitiesLayer(nafEntities, getName());
    }

    public void convertTo(String outDir) throws AbnormalProcessException {
        List<Entity> nafEntities = entityAligner.align();
        refNaf.createEntitiesLayer(nafEntities, getName());
        refNaf.writeToDir(outDir);
        logStats();
    }


    public static void run(Path file, List<String> dirs) throws AbnormalProcessException {
        File refFile = IO.findFileWithSameId(file, new File(dirs.get(0)));
        NafXmiReader nafXmiReader = NafXmiReader.createTeiTextReader(refFile.getPath(), file.toString());
        nafXmiReader.convertTo(dirs.get(1));
    }

    public static void runWithConnl2Xmi(Path file, List<String> dirs) throws AbnormalProcessException {
        File refFile = IO.findFileWithSameId(file, new File(dirs.get(0)));
        Conll2Xmi conll2Xmi = new Conll2Xmi(file.toString());
        conll2Xmi.convert();
        NafXmiReader nafXmiReader = NafXmiReader.createTeiTextReader(refFile.getPath(), conll2Xmi.getXmi());
        nafXmiReader.convertTo(dirs.get(1));
    }

    private void logStats() {
        entityAligner.logStats();
    }


    public void write(String outFile) throws AbnormalProcessException {
        refNaf.write(outFile);
    }

    public String getName() {
        return Handler.NAME + "-" + NAME;
    }

}
