package logging;

public interface LoggerListener {
	public void RawUpdated();
	public void MinuteUpdated();
	public void HourUpdated();
}
