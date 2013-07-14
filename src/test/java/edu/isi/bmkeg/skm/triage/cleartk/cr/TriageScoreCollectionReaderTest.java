package edu.isi.bmkeg.skm.triage.cleartk.cr;

import static java.util.Arrays.asList;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.util.CasCreationUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.uimafit.util.CasUtil;

import edu.isi.bmkeg.triage.uimaTypes.TriageScore;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/edu/isi/bmkeg/skm/triage/appCtx-VPDMfTest.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD) // Forcing the initialization of the ApplicationContext after each test.
															// This is needed to provide a clean dao instance and a blank db which is
															// produced during the application context initialization.
public class TriageScoreCollectionReaderTest {

	private static String test_triage_corpus_name = "Small"; 
	private static String test_target_A_corpus_name = "AP"; 
	private static int test_corpus_A_in_triage_cnt = 2; 
	private static int test_corpus_A_out_triage_cnt = 3; 
	private static int test_corpus_A_unknown_triage_cnt = 0; 
	private static int test_corpus_A_in_all_cnt = 4; 
	private static int test_corpus_A_out_all_cnt = 4; 
	private static int test_corpus_A_unknown_all_cnt = 1; 
	private static int test_corpus_no_doc_cnt = 0; 
	
	@Autowired
	private BmkegProperties prop;

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@Test
	public void testTriagedDocumentCollectionReader_corpusA_triage() throws Exception {
		
		CollectionReader cr = TriageScoreCollectionReader.load(
				test_triage_corpus_name, test_target_A_corpus_name,
				prop.getDbUser(),
				prop.getDbPassword(),
				prop.getDbUrl()+ "_triage");

		int inCnt = 0;
		int outCnt = 0;
		int unknownCnt = 0;
		int noDocCnt = 0;
		
		final CAS cas = CasCreationUtils.createCas(asList(cr.getMetaData()));
		
		try {
			// Process
			while (cr.hasNext()) {
				cr.getNext(cas);
				
				String doc = cas.getDocumentText();
				
				if (doc == null || doc.length() == 0)
					noDocCnt++;

				TriageScore cit = (TriageScore) CasUtil.selectSingle(cas, CasUtil.getType(cas, TriageScore.class));
				Assert.assertNotNull(cit);
				Assert.assertTrue(cit.getCitation_id() > 0);
				Assert.assertTrue(cit.getVpdmfId() > 0);
				Assert.assertNotNull(cit.getInOutCode());
				
				if (cit.getInOutCode().equals("in")) inCnt++; 
				else if (cit.getInOutCode().equals("out")) outCnt++; 
				else unknownCnt++;
				cas.reset();
			}
			
		}
		finally {
			// Destroy
			cr.destroy();
		}
		
		Assert.assertEquals(test_corpus_A_unknown_triage_cnt, unknownCnt);				
		Assert.assertEquals(test_corpus_A_in_triage_cnt, inCnt);				
		Assert.assertEquals(test_corpus_A_out_triage_cnt, outCnt);				
		Assert.assertEquals(test_corpus_no_doc_cnt, noDocCnt);				

	}

	@Test
	public void testTriagedDocumentCollectionReader_corpusA_all() throws Exception {
		
		CollectionReader cr = TriageScoreCollectionReader.load(
				test_target_A_corpus_name,
				prop.getDbUser(),
				prop.getDbPassword(),
				prop.getDbUrl()+ "_triage");

		int inCnt = 0;
		int outCnt = 0;
		int unknownCnt = 0;
		int noDocCnt = 0;
		
		final CAS cas = CasCreationUtils.createCas(asList(cr.getMetaData()));
		
		try {
			// Process
			while (cr.hasNext()) {
				cr.getNext(cas);
				
				String doc = cas.getDocumentText();
				
				if (doc == null || doc.length() == 0)
					noDocCnt++;

				TriageScore cit = (TriageScore) CasUtil.selectSingle(cas, CasUtil.getType(cas, TriageScore.class));
				Assert.assertNotNull(cit);
				Assert.assertTrue(cit.getCitation_id() > 0);
				Assert.assertTrue(cit.getVpdmfId() < 0);
				Assert.assertNotNull(cit.getInOutCode());
				
				if (cit.getInOutCode().equals("in")) inCnt++; 
				else if (cit.getInOutCode().equals("out")) outCnt++; 
				else unknownCnt++;
				cas.reset();
			}
			
		}
		finally {
			// Destroy
			cr.destroy();
		}
		
		Assert.assertEquals(test_corpus_A_unknown_all_cnt, unknownCnt);				
		Assert.assertEquals(test_corpus_A_in_all_cnt, inCnt);				
		Assert.assertEquals(test_corpus_A_out_all_cnt, outCnt);				
		Assert.assertEquals(test_corpus_no_doc_cnt, noDocCnt);				

	}

	@Test
	public void testTriagedDocumentCollectionReaderSkipUnknowns_corpusA_triage() throws Exception {
		
		CollectionReader cr = TriageScoreCollectionReader.load(
				test_triage_corpus_name, test_target_A_corpus_name,
				prop.getDbUser(),
				prop.getDbPassword(),
				prop.getDbUrl()+ "_triage",
				true);

		int inCnt = 0;
		int outCnt = 0;
		int unknownCnt = 0;
		int noDocCnt = 0;
		
		final CAS cas = CasCreationUtils.createCas(asList(cr.getMetaData()));
		
		try {
			// Process
			while (cr.hasNext()) {
				cr.getNext(cas);
				
				String doc = cas.getDocumentText();
				
				if (doc == null || doc.length() == 0)
					noDocCnt++;

				TriageScore cit = (TriageScore) CasUtil.selectSingle(cas, CasUtil.getType(cas, TriageScore.class));
				Assert.assertTrue(cit.getCitation_id() > 0);
				Assert.assertNotNull(cit);
				Assert.assertTrue(cit.getVpdmfId() > 0);
				Assert.assertNotNull(cit.getInOutCode());
				
				if (cit.getInOutCode().equals("in")) inCnt++; 
				else if (cit.getInOutCode().equals("out")) outCnt++; 
				else unknownCnt++;
								
				cas.reset();
			}
			
		}
		finally {
			// Destroy
			cr.destroy();
		}
		
		Assert.assertEquals(0, unknownCnt);				
		Assert.assertEquals(test_corpus_A_in_triage_cnt, inCnt);				
		Assert.assertEquals(test_corpus_A_out_triage_cnt, outCnt);				
		Assert.assertEquals(test_corpus_no_doc_cnt, noDocCnt);				

	}

	@Test
	public void testTriagedDocumentCollectionReaderSkipUnknowns_corpusA_all() throws Exception {
		
		CollectionReader cr = TriageScoreCollectionReader.load(
				test_target_A_corpus_name,
				prop.getDbUser(),
				prop.getDbPassword(),
				prop.getDbUrl()+ "_triage",
				true);

		int inCnt = 0;
		int outCnt = 0;
		int unknownCnt = 0;
		int noDocCnt = 0;
		
		final CAS cas = CasCreationUtils.createCas(asList(cr.getMetaData()));
		
		try {
			// Process
			while (cr.hasNext()) {
				cr.getNext(cas);
				
				String doc = cas.getDocumentText();
				
				if (doc == null || doc.length() == 0)
					noDocCnt++;

				TriageScore cit = (TriageScore) CasUtil.selectSingle(cas, CasUtil.getType(cas, TriageScore.class));
				Assert.assertNotNull(cit);
				Assert.assertTrue(cit.getCitation_id() > 0);
				Assert.assertTrue(cit.getVpdmfId() < 0);
				Assert.assertNotNull(cit.getInOutCode());
				
				if (cit.getInOutCode().equals("in")) inCnt++; 
				else if (cit.getInOutCode().equals("out")) outCnt++; 
				else unknownCnt++;
								
				cas.reset();
			}
			
		}
		finally {
			// Destroy
			cr.destroy();
		}
		
		Assert.assertEquals(0, unknownCnt);				
		Assert.assertEquals(test_corpus_A_in_all_cnt, inCnt);				
		Assert.assertEquals(test_corpus_A_out_all_cnt, outCnt);				
		Assert.assertEquals(test_corpus_no_doc_cnt, noDocCnt);				

	}

}
