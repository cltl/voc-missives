import utils.map_INTids_to_TF_file_ids as int_map


def test_intid_mapping_for_json_creation():
    input_file = 'vol1_p0106_INT_08c82040-752f-3fc2-ad50-0d3e7b37a945_text.naf'
    v, p, id, tt = int_map.match_vpint('vol1_p0106_INT_08c82040-752f-3fc2-ad50-0d3e7b37a945_text.naf')
    assert v == 1
    assert p == 106
    assert id == 'INT_08c82040-752f-3fc2-ad50-0d3e7b37a945'
    assert tt == 'text'

    tf_ids = int_map.get_tf_ids([(v, p, id, tt)])
    assert tf_ids[0] == 'missive_1_49_text'

    json_obj = int_map.json_obj([input_file], [(v, p, id, tt)], tf_ids)
    assert len(json_obj) == 1


def test_mapping_manual():
    inputfile = 'INT_08c82040-752f-3fc2-ad50-0d3e7b37a945.xmi'
    i, t, e = int_map.match_int(inputfile)
    assert i == 'INT_08c82040-752f-3fc2-ad50-0d3e7b37a945'
    assert t == 'text'
    assert e == 'xmi'
    trgnames = int_map.get_target_filenames([(i, t, e)], int_map.load('resources/annotated.json'))
    assert trgnames[0] == 'missive_1_49_text.xmi'

