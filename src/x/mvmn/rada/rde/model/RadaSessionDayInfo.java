package x.mvmn.rada.rde.model;

import java.util.function.Function;

import x.mvmn.rada.rde.extract.RadaContentHelper;
import x.mvmn.rada.rde.extract.RadaContentHelper.ExtractUrlParam;

public class RadaSessionDayInfo implements Comparable<RadaSessionDayInfo> {

	protected final int year;
	protected final int month;
	protected final int day;
	protected final int session;

	public static Function<String, RadaSessionDayInfo> FROM_SESSION_DAY_URL = new Function<String, RadaSessionDayInfo>() {
		protected final ExtractUrlParam EXTRACT_URL_PARAM_day_ = new ExtractUrlParam("day_");
		protected final ExtractUrlParam EXTRACT_URL_PARAM_month_ = new ExtractUrlParam("month_");
		protected final ExtractUrlParam EXTRACT_URL_PARAM_year = new ExtractUrlParam("year");
		protected final ExtractUrlParam EXTRACT_URL_PARAM_nom_s = new ExtractUrlParam("nom_s");

		@Override
		public RadaSessionDayInfo apply(String url) {
			return new RadaSessionDayInfo(RadaContentHelper.FN_PARSE_INT.apply(EXTRACT_URL_PARAM_year.apply(url)),
					RadaContentHelper.FN_PARSE_INT.apply(EXTRACT_URL_PARAM_month_.apply(url)),
					RadaContentHelper.FN_PARSE_INT.apply(EXTRACT_URL_PARAM_day_.apply(url)),
					RadaContentHelper.FN_PARSE_INT.apply(EXTRACT_URL_PARAM_nom_s.apply(url)));
		}
	};

	public static RadaSessionDayInfo fromSessionDayUrl(String url) {
		return FROM_SESSION_DAY_URL.apply(url);
	}

	public RadaSessionDayInfo(int year, int month, int day, int session) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.session = session;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}

	public int getSession() {
		return session;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RadaSessionDayInfo [year=").append(year).append(", month=").append(month).append(", day=").append(day).append(", session=")
				.append(session).append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 8831;
		int result = 1;
		result = prime * result + session;
		result = prime * result + year;
		result = prime * result + month;
		result = prime * result + day;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RadaSessionDayInfo other = (RadaSessionDayInfo) obj;
		if (day != other.day)
			return false;
		if (month != other.month)
			return false;
		if (session != other.session)
			return false;
		if (year != other.year)
			return false;
		return true;
	}

	@Override
	public int compareTo(RadaSessionDayInfo o) {
		if (o == null) {
			return -1;
		}

		return (this.session - o.session) * 100000000 + (this.year - o.year) * 10000 + (this.month - o.month) * 100 + this.day - o.day;
	}
}
