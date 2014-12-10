/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.stock.reports;

import static play.data.Form.form;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import models.Contact;
import models.GlobalPrivateCode;
import models.GlobalTransPoint;
import models.SaleSeller;
import models.StockDepot;
import models.StockTransSource;
import play.data.Form;
import play.data.format.Formats.DateTime;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import reports.ReportParams;
import reports.ReportService;
import reports.ReportService.ReportResult;
import utils.AuthManager;
import utils.CacheUtils;
import utils.DateUtils;
import utils.InstantSQL;
import views.html.stocks.reports.receipt_list;
import controllers.global.Profiles;
import enums.ReportUnit;
import enums.Right;
import enums.RightLevel;

/**
 * @author mdpinar
*/
public class ReceiptList extends Controller {

	private final static Right RIGHT_SCOPE = Right.STOK_FIS_LISTESI;
	private final static String REPORT_NAME = "ReceiptList";

	private final static Form<ReceiptList.Parameter> parameterForm = form(ReceiptList.Parameter.class);

	public static class Parameter {

		public String orderBy;
		public ReportUnit unit;

		public Contact contact;

		public GlobalTransPoint transPoint = Profiles.chosen().gnel_transPoint;
		public GlobalPrivateCode privateCode = Profiles.chosen().gnel_privateCode;

		public Right transType;
		public String transNo;
		public StockTransSource transSource;

		@DateTime(pattern = "dd/MM/yyyy")
		public Date startDate = DateUtils.getFirstDayOfMonth();

		@DateTime(pattern = "dd/MM/yyyy")
		public Date endDate = new Date();

		@DateTime(pattern = "dd/MM/yyyy")
		public Date deliveryDate;

		public StockDepot depot;
		public SaleSeller seller;

		public String showType;

		public static Map<String, String> options() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
			options.put("t.trans_date", Messages.get("date"));
			options.put("t.contact_name, t.trans_date", Messages.get("contact.name"));
			options.put("t.delivery_date", Messages.get("date.delivery") + " " + Messages.get("ascending"));
			options.put("t.delivery_date desc", Messages.get("date.delivery") + " " + Messages.get("descending"));

			return options;
		}

		public static Map<String, String> showTypes() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();

			options.put("Summary", Messages.get("report.show.summary"));
			options.put("Detailed", Messages.get("report.show.detail"));

			return options;
		}

	}

	private static String getQueryString(Parameter params) {
		StringBuilder queryBuilder = new StringBuilder(" and ref_id is null");

		queryBuilder.append(" and t.workspace = " + CacheUtils.getWorkspaceId());

		if (params.contact != null && params.contact.id != null) {
			queryBuilder.append(" and contact_id = ");
			queryBuilder.append(params.contact.id);
		}

		if (params.startDate != null) {
			queryBuilder.append(" and t.trans_date >= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.startDate));
		}

		if (params.endDate != null) {
			queryBuilder.append(" and t.trans_date <= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.endDate));
		}

		if (params.deliveryDate != null) {
			queryBuilder.append(" and delivery_date = ");
			queryBuilder.append(DateUtils.formatDateForDB(params.deliveryDate));
		}

		if (params.depot != null && params.depot.id != null) {
			queryBuilder.append(" and depot_id = ");
			queryBuilder.append(params.depot.id);
		}

		if (params.seller != null && params.seller.id != null) {
			queryBuilder.append(" and seller_id = ");
			queryBuilder.append(params.seller.id);
		}

		if (params.transType != null) {
			queryBuilder.append(" and _right = '");
			queryBuilder.append(params.transType);
			queryBuilder.append("'");
		}

		if (params.transNo != null && ! params.transNo.isEmpty()) {
			queryBuilder.append(" and trans_no = '");
			queryBuilder.append(params.transNo);
			queryBuilder.append("'");
		}

		if (params.transSource != null && params.transSource.id != null) {
			queryBuilder.append(" and trans_source_id = ");
			queryBuilder.append(params.transSource.id);
		}

		return queryBuilder.toString();
	}

	public static Result generate() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		Form<ReceiptList.Parameter> filledForm = parameterForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			return badRequest(receipt_list.render(filledForm));
		} else {

			Parameter params = filledForm.get();

			ReportParams repPar = new ReportParams();
			repPar.modul = RIGHT_SCOPE.module.name();
			repPar.reportName = REPORT_NAME + params.showType;
			repPar.reportUnit = params.unit;
			repPar.query = getQueryString(params);
			repPar.orderBy = params.orderBy;

			/*
			 * Parametrik degerlerin gecisi
			 */
			repPar.paramMap.put("TRANS_POINT_SQL", "");
			if (params.transPoint != null && params.transPoint.id != null) {
				repPar.paramMap.put("TRANS_POINT_SQL", InstantSQL.buildTransPointSQL(params.transPoint.id));
			}
			repPar.paramMap.put("PRIVATE_CODE_SQL", "");
			if (params.privateCode != null && params.privateCode.id != null) {
				repPar.paramMap.put("PRIVATE_CODE_SQL", InstantSQL.buildPrivateCodeSQL(params.privateCode.id));
			}
			repPar.paramMap.put("REPORT_INFO", Messages.get("report.info.date_range", DateUtils.formatDateStandart(params.startDate), DateUtils.formatDateStandart(params.endDate)));

			ReportResult repRes = ReportService.generateReport(repPar, response());
			if (repRes.error != null) {
				flash("warning", repRes.error);
				return ok(receipt_list.render(filledForm));
			} else {
				return ok(repRes.stream);
			}
		}

	}

	public static Result index() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		return ok(receipt_list.render(parameterForm.fill(new Parameter())));
	}

}
