#!/usr/sbin/dtrace -Cqs
long milliintr;

BEGIN{
    printf("#Measure 'usr+sys+intr / total' on a system in per thousand of a CPU\n");
	printf("#..output to be parsed later by awk script to calculate average cpu utilization\n");
    printf("#...modified Roch's script (Sat Jul 30 19:43:29 PDT 2005)\n");
    usr = 0;
    sys = 0;
    intr = 0;
    idle = 0;

    ticks = 0;
    t0=timestamp;	 
    t00 = t0;
    cpux = cpu;
}

profile-1ms {
    usr += (arg1 != 0) ? 1 : 0;
    sys += (arg0 != 0) ? 1 : 0; /* sys + intr +idle */
    idle += (curthread == curthread->t_cpu->cpu_idle_thread) ? 1 : 0;
    intr += ((curthread->t_cpu->cpu_intr_actv - 16384) > 0) ? 1 : 0;
    ticks += (cpu == cpux) ? 1 : 0;
}

tick-1s{
    milliusr = (1000 * usr) / ticks;
    millisys = (1000 * (sys - intr - idle)) / ticks;
    milliidle = (1000 * idle) / ticks;
    milliintr = (1000 * intr) / ticks;
    millittot = milliusr + millisys + milliintr + milliidle;

    /*printf("%d + %d + %d  / %7d\n", milliusr, millisys, milliintr, millittot );*/
    printf("%d + %d + %d  / %7d\n", milliusr*100/millittot, millisys*100/millittot, milliintr*100/millittot, millittot );

    idle = 0;
    usr = 0;
    intr = 0;
    sys = 0;

    ticks = 0;
    t0=timestamp; 
}
