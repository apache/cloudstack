package org.apache.cloudstack.fizzbuzz;

import com.cloud.fizzbuzz.FizzBuzzService;
import com.cloud.server.StatsCollector;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.user.fizzbuzz.FizzBuzzCmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FizzBuzzServiceImpl extends ManagerBase implements FizzBuzzService, PluggableService {
    @Override
    public String getDisplayText(Integer n) {
        return (n == null) ? getFizzBuzzOnVms() : getFizzBuzzOnN(n);
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(FizzBuzzCmd.class);
        return cmdList;
    }

    private String getFizzBuzzOnVms() {
        return getFizzBuzzOnN(StatsCollector.getInstance().getNumberOfVms());
    }

    private String getFizzBuzzOnN(int n) {
        return (n % 15 == 0) ? "fizzbuzz" : (n % 3 == 0) ? "fizz" : (n % 5 == 0) ? "buzz" : String.valueOf(new Random().nextInt(99) + 1);
    }
}
