package entities.entityIntegration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.common.AbnormalProcessException;
import utils.common.IO;
import utils.naf.NafDoc;
import utils.xmi.CasDoc;

/**
 * Integrates manual entity annotations from an XMI file into
 * a reference NAF file. This NAF file is derived from an input NAF,
 * by filtering text or notes content (text units). The reference
 * NAF file further adds a tokens layer.
 * Entities from the XMI file are mapped against the raw text of the
 * reference NAF, and then matched to overlapping tokens. The reference
 * NAF is accordingly enriched with an entities layer.
 */
public class NafXmiReader {
    String textType;
    NafDoc inNaf;
    NafDoc refNaf;
    CasDoc xmi;
    private static final String IN = "." + IO.XMI_SFX;
    private static final String OUT = "." + IO.NAF_SFX;

    public static final Logger logger = LogManager.getLogger(NafXmiReader.class);

    private NafXmiReader(String inputNaf, String inputXmi) throws AbnormalProcessException {
        xmi = CasDoc.create(inputXmi);
        inNaf = NafDoc.create(inputNaf);
    }

    private void filterInputNaf() {
//        refNaf =
    }
}
