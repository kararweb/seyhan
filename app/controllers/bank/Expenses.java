/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.bank;

import static play.data.Form.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import meta.GridHeader;
import meta.PageExtend;
import models.BankExpense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AuthManager;
import utils.CacheUtils;
import utils.StringUtils;
import views.html.banks.expense.form;
import views.html.banks.expense.index;
import views.html.banks.expense.list;
import controllers.Application;
import controllers.global.Profiles;
import enums.Right;
import enums.RightLevel;

/**
 * @author mdpinar
*/
public class Expenses extends Controller {

	private final static Right RIGHT_SCOPE = Right.BANK_MASRAF_TANITIMI;

	private final static Logger log = LoggerFactory.getLogger(Expenses.class);
	private final static Form<BankExpense> dataForm = form(BankExpense.class);

	private static String lastSaved;

	/**
	 * Liste formu basliklarini doner
	 * 
	 * @return List<GridHeader>
	 */
	private static List<GridHeader> getHeaderList() {
		List<GridHeader> headerList = new ArrayList<GridHeader>();
		headerList.add(new GridHeader(Messages.get("name"), true).sortable("name"));
		headerList.add(new GridHeader(Messages.get("is_active"), "7%", true));

		return headerList;
	}

	/**
	 * Liste formunda gosterilecek verileri doner
	 * 
	 * @return PageExtend
	 */
	private static PageExtend<BankExpense> buildPage() {
		List<Map<Integer, String>> dataList = new ArrayList<Map<Integer, String>>();

		List<BankExpense> modelList = BankExpense.page();
		if (modelList != null && modelList.size() > 0) {
			for (BankExpense model : modelList) {
				Map<Integer, String> dataMap = new HashMap<Integer, String>();
				int i = -1;
				dataMap.put(i++, model.id.toString());
				dataMap.put(i++, model.name);
				dataMap.put(i++, model.isActive.toString());

				dataList.add(dataMap);
			}
		}

		return new PageExtend<BankExpense>(getHeaderList(), dataList, null);
	}

	public static Result index() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		return ok(
			index.render(buildPage())
		);
	}

	public static Result options() {
		Result result = ok(StringUtils.buildOptionTag(BankExpense.options(), lastSaved));
		lastSaved = null;

		return result;
	}

	/**
	 * Uzerinde veri bulunan liste formunu doner
	 */
	public static Result list() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		return ok(
			list.render(buildPage())
		);
	}

	/**
	 * Kayit formundaki bilgileri kaydeder
	 */
	public static Result save() {
		if (! CacheUtils.isLoggedIn()) return Application.login();

		Form<BankExpense> filledForm = dataForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			return badRequest(form.render(filledForm));
		} else {

			BankExpense model = filledForm.get();

			Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, (model.id == null ? RightLevel.Insert : RightLevel.Update));
			if (hasProblem != null) return hasProblem;

			String editingConstraintError = model.checkEditingConstraints();
			if (editingConstraintError != null) return badRequest(editingConstraintError);

			checkConstraints(filledForm);

			if (filledForm.hasErrors()) {
				return badRequest(form.render(filledForm));
			}

			try {
				if (model.id == null) {
					model.save();
				} else {
					model.update();
				}
			} catch (OptimisticLockException e) {
				flash("error", Messages.get("exception.optimistic.lock"));
				return badRequest(form.render(dataForm.fill(model)));
			}
			lastSaved = model.name;

			flash("success", Messages.get("saved", model.name));
			if (Profiles.chosen().gnel_continuouslyRecording)
				return create();
			else
				return ok();
		}
	}

	/**
	 * Yeni bir kayit formu olusturur
	 */
	public static Result create() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Insert);
		if (hasProblem != null) return hasProblem;

		return ok(form.render(dataForm.fill(new BankExpense(""))));
	}

	/**
	 * Secilen kayit icin duzenleme formunu acar
	 * 
	 * @param id
	 */
	public static Result edit(Integer id) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		if (id == null) {
			return badRequest(Messages.get("id.is.null"));
		} else {
			BankExpense model = BankExpense.findById(id);
			if (model == null) {
				return badRequest(Messages.get("not.found", Messages.get("expense")));
			} else {
				return ok(form.render(dataForm.fill(model)));
			}
		}
	}

	/**
	 * Duzenlemek icin acilmis olan kaydi siler
	 * 
	 * @param id
	 */
	public static Result remove(Integer id) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Delete);
		if (hasProblem != null) return hasProblem;

		if (id == null) {
			return badRequest(Messages.get("id.is.null"));
		} else {
			BankExpense model = BankExpense.findById(id);
			if (model == null) {
				return badRequest(Messages.get("not.found", Messages.get("expense")));
			} else {
				String editingConstraintError = model.checkEditingConstraints();
				if (editingConstraintError != null) return badRequest(editingConstraintError);
				try {
					model.delete();
					flash("success", Messages.get("deleted", model.name));
					return ok();
				} catch (PersistenceException pe) {
					log.error("ERROR", pe);
					flash("error", Messages.get("delete.violation", model.name));
					return badRequest(Messages.get("delete.violation", model.name));
				}
			}
		}
	}

	/**
	 * Kayit isleminden once form uzerinde bulunan verilerin uygunlugunu kontrol eder
	 * 
	 * @param filledForm
	 */
	private static void checkConstraints(Form<BankExpense> filledForm) {
		BankExpense model = filledForm.get();

		if (BankExpense.isUsedForElse("name", model.name, model.id)) {
			filledForm.reject("name", Messages.get("not.unique", model.name));
		}
	}

}
