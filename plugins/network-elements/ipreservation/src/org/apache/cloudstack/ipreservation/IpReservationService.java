package org.apache.cloudstack.ipreservation;

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.ipreservation.api.commands.AddIpReservationCmd;
import org.apache.cloudstack.ipreservation.api.commands.ListIpReservationCmd;
import org.apache.cloudstack.ipreservation.api.commands.RemoveIpReservationCmd;

public interface IpReservationService extends PluggableService {
    void createReservation(AddIpReservationCmd cmd);

    void getReservations(ListIpReservationCmd cmd);

    void removeReservation(RemoveIpReservationCmd cmd);
}
