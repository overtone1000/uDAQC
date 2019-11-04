package threading;

public abstract class ThreadWorker implements Runnable
{
	public ThreadWorker()
	{
		t=new Thread(this);
	}
	public ThreadWorker(String name)
	{
		t=new Thread(this,name);
	}
	private Thread t=new Thread(this);
	
	private boolean continue_running=true;
	public void stop() throws InterruptedException
	{
		continue_running=false;
		t.join(10000);
		if(t.isAlive())
		{
			System.out.println("Thread is still running after attempted join. Interrupting.");
			t.interrupt();
		}
	}
	public void start()
	{
		continue_running=true;
		t.start();
	}
	public boolean continueRunning()
	{
		return continue_running;
	}
}	