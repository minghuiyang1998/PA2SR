import java.util.*;

public class EventListImpl implements EventList {
    private List<Event> data;

    public EventListImpl() {
        data = new ArrayList<>();
    }

    public boolean add(Event e) {
        data.add(e);
        return true;
    }

    public Event removeNext() {
        if (data.isEmpty()) {
            return null;
        }

        int firstIndex = 0;
        double first = ((Event)data.get(firstIndex)).getTime();
        for (int i = 0; i < data.size(); i++) {
            if (((Event)data.get(i)).getTime() < first)
            {
                first = ((Event)data.get(i)).getTime();
                firstIndex = i;
            }
        }

        Event next = (Event)data.get(firstIndex);
        data.remove(next);

        return next;
    }

    public String toString() {
        return data.toString();
    }

    public Event removeTimer(int entity) {
        int timerIndex = -1;
        Event timer = null;

        for (int i = 0; i < data.size(); i++) {
            if ((((Event)(data.get(i))).getType() ==
                    NetworkSimulator.TIMERINTERRUPT) &&
                    (((Event)(data.get(i))).getEntity() == entity))
            {
                timerIndex = i;
                break;
            }
        }

        if (timerIndex != -1) {
            timer = (Event)(data.get(timerIndex));
            data.remove(timer);
        }

        return timer;

    }

    public double getLastPacketTime(int entityTo) {
        double time = 0;
        for (int i = 0; i < data.size(); i++) {
            if ((((Event)(data.get(i))).getType() ==
                    NetworkSimulator.FROMLAYER3) &&
                    (((Event)(data.get(i))).getEntity() == entityTo))
            {
                time = ((Event)(data.get(i))).getTime();
            }
        }

        return time;
    }
}