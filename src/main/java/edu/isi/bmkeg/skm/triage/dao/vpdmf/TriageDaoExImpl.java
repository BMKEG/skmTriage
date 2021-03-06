package edu.isi.bmkeg.skm.triage.dao.vpdmf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.WebApplicationContext;

import edu.isi.bmkeg.skm.triage.dao.TriageDaoEx;
import edu.isi.bmkeg.triage.model.TriageCorpus;
import edu.isi.bmkeg.triage.model.qo.TriageCorpus_qo;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.ChangeEngineImpl;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.ChangeEngine;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

@Repository
public class TriageDaoExImpl implements TriageDaoEx {

	private static Logger logger = Logger.getLogger(TriageDaoExImpl.class);

	@Autowired
	private CoreDao coreDao;

	// ~~~~~~~~~~~~
	// Constructors
	// ~~~~~~~~~~~~
	public TriageDaoExImpl() {}

	public TriageDaoExImpl(CoreDao coreDao) {
		this.coreDao = coreDao;
	}

	// ~~~~~~~~~~~~~~~~~~~
	// Getters and Setters
	// ~~~~~~~~~~~~~~~~~~~
	public void setCoreDao(CoreDao coreDao) {
		this.coreDao = coreDao;
	}

	public CoreDao getCoreDao() {
		return coreDao;
	}

	private ChangeEngine getCe() {
		return coreDao.getCe();
	}

	private VPDMf getTop() {
		return coreDao.getTop();
	}

	// ~~~~~~~~~~~~~~~
	// Count functions
	// ~~~~~~~~~~~~~~~

	@Override
	public int countTriagedArticlesInCorpus(String corpusName) throws Exception {

		int count = 0;
	
		try {
	
			getCe().connectToDB();
			getCe().turnOffAutoCommit();
			
			ViewDefinition vd = getTop().getViews().get("TriagedArticle");
			ViewInstance vi = new ViewInstance(vd);
			AttributeInstance ai = vi.readAttributeInstance(
					"]TriageCorpus|TriageCorpus.name", 0);
			ai.writeValueString(corpusName);
			
			count = getCe().executeCountQuery(vi);
		
		} finally {
			getCe().closeDbConnection();
		}

		return count;
	
	}
	
	// ~~~~~~~~~~~~~~~~~~~
	// Insert Functions
	// ~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~
	// Update Functions
	// ~~~~~~~~~~~~~~~~~~~
	
	// ~~~~~~~~~~~~~~~~~~~
	// Delete Functions
	// ~~~~~~~~~~~~~~~~~~~
	@Override
	public void deleteExistingTriageScores(String triageCorpus, 
			String targetCorpus) throws Exception {

		
		//
		// REMOVE EXISTING DATA FROM THE TRIAGE SCORE
		// (AND TRIAGEFEATURE) TABLE.
		// NEED TO UPDATE THE DELETION FUNCTIONS WITHIN VPDMf
		//
		String sql1 = "DELETE tf.* " + "FROM TriageScore AS ts, "
				+ " TriageFeature AS tf, " + " Corpus AS targetc, "
				+ " Corpus AS triagec "
				+ "WHERE ts.targetCorpus_id = targetc.vpdmfId "
				+ "  AND targetc.name = '" + targetCorpus + "'"
				+ "  AND ts.triageCorpus_id = triagec.vpdmfId "
				+ "  AND triagec.name = '" + triageCorpus + "';";

		int nRowsChanged = this.getCoreDao().getCe()
				.executeRawUpdateQuery(sql1);
		this.coreDao.getCe().prettyPrintSQL(sql1);
		logger.debug(nRowsChanged + " rows altered.");

		String sql2 = "DELETE ts.*, vt.* " + "FROM TriageScore AS ts, "
				+ " ViewTable AS vt, " + " Corpus AS targetc, "
				+ " Corpus AS triagec " + "WHERE vt.vpdmfId = ts.vpdmfId "
				+ "  AND ts.targetCorpus_id = targetc.vpdmfId "
				+ "  AND targetc.name = '" + targetCorpus + "'"
				+ "  AND ts.triageCorpus_id = triagec.vpdmfId "
				+ "  AND triagec.name = '" + triageCorpus + "';";

		nRowsChanged = this.getCoreDao().getCe().executeRawUpdateQuery(sql2);
		this.coreDao.getCe().prettyPrintSQL(sql2);
		logger.debug(nRowsChanged + " rows altered.");
			
	}
	
	// ~~~~~~~~~~~~~~~~~~~~
	// Find by id Functions
	// ~~~~~~~~~~~~~~~~~~~~

	@Override
	public TriageCorpus findTriageCorpusByName(String name) throws Exception {

		TriageCorpus_qo tQo = new TriageCorpus_qo();
		tQo.setName(name);
		
		List<LightViewInstance> lviList = coreDao.list(tQo, "TriageCorpus");
		if( lviList.size() != 1 )
			return null;

		TriageCorpus tc = coreDao.findById(lviList.get(0).getVpdmfId(), new TriageCorpus(), "TriageCorpus");
		
		return tc;

	}
	
	// ~~~~~~~~~~~~~~~~~~~~
	// Retrieve functions
	// ~~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~
	// List functions
	// ~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~~
	// Add x to y functions
	// ~~~~~~~~~~~~~~~~~~~~
	
	// TODO: CONVERT THIS TO A SIMPLER, QUICKER BATCH UPLOAD FUNCTION.
	@Override
	public void addTriageDocumentsToCorpus(String triageCorpus, 
			String targetCorpus,
			Map<Integer, String> pmidCodes) throws Exception {

		int count = 0;
		long t = System.currentTimeMillis();
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = df.format(new Date());
		
		ChangeEngineImpl ce = (ChangeEngineImpl) this.coreDao.getCe();
		VPDMf top = ce.readTop();

		ViewDefinition vd = top.getViews().get("TriagedArticle");
		ViewDefinition articleVd = top.getViews().get("ArticleCitation");

		List<Integer> pmids = new ArrayList<Integer>(pmidCodes.keySet());
		Collections.sort(pmids);
		Iterator<Integer> it = pmids.iterator();
		while (it.hasNext()) {
			Integer pmid = it.next();

			ViewInstance qvi = new ViewInstance(articleVd);
			AttributeInstance ai = qvi.readAttributeInstance(
					"]LiteratureCitation|ArticleCitation.pmid", 0);
			ai.writeValueString(pmid + "");
			List<LightViewInstance> l = ce.executeListQuery(qvi);
			if( l.size() == 0 ) {
				continue;
			} else if( l.size() > 1 ) {
				throw new Exception("PMID " + pmid + " ambiguous.");
			}
			LightViewInstance lvi = l.get(0);
				
			ViewInstance vi = new ViewInstance(vd);

			ai = vi.readAttributeInstance(
					"]TriageCorpus|Corpus.name", 0);
			ai.writeValueString(triageCorpus);

			ai = vi.readAttributeInstance(
					"]TargetCorpus|Corpus.name", 0);
			ai.writeValueString(targetCorpus);
			
			String code = pmidCodes.get(pmid);
			
			ai = vi.readAttributeInstance(
					"]LiteratureCitation|ViewTable.vpdmfLabel", 0);
			ai.writeValueString(lvi.getVpdmfLabel());

			ai = vi.readAttributeInstance(
					"]LiteratureCitation|ViewTable.vpdmfId", 0);
			ai.writeValueString(lvi.getVpdmfId() + "");
			
			count++;

			if( (count % 50 == 0) )
				logger.info("Updated " + count + " / " + pmids.size() 
						+ " documents in " + 
						(System.currentTimeMillis() - t) / 1000.0 + " s");
			
			ai = vi.readAttributeInstance(
					"]TriageScore|TriageScore.inOutCode", 0);
			ai.setValue(code);

			ai = vi.readAttributeInstance(
					"]TriageScore|TriageScore.inScore", 0);
			ai.writeValueString("-1");

			ai = vi.readAttributeInstance(
					"]TriageScore|TriageScore.classifyTimestamp", 0);
			ai.writeValueString(timestamp);
			
			ai = vi.readAttributeInstance(
					"]TriageScore|TriageScore.scoredTimestamp", 0);
			ai.writeValueString(timestamp);
			
			getCe().executeInsertQuery(vi);

		}

		long deltaT = System.currentTimeMillis() - t;
		logger.info("Added " + count + " entries in " + deltaT / 1000.0 + " s\n");

	}

}
