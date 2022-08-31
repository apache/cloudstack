package com.cloud.resourcelimit;

import com.cloud.configuration.Resource;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CheckedReservationTest {

    @Mock
    Account account;
    @Mock
    ReservationDao reservationDao;
    @Mock
    ResourceLimitService resourceLimitService;

    @Mock
    ReservationVO reservation = new ReservationVO(1l, 1l, Resource.ResourceType.user_vm, 1l);

    @Before
    public void setup() {
        initMocks(this);
        when(reservation.getId()).thenReturn(1l);
    }
    @Test
    public void getId() {
        when(reservationDao.persist(any())).thenReturn(reservation);
        when(account.getAccountId()).thenReturn(1l);
        boolean fail = false;
        long id = 0l;
        try (CheckedReservation cr = new CheckedReservation(account, Resource.ResourceType.user_vm,1l, reservationDao, resourceLimitService); ) {
            id = cr.getId();
        } catch (NullPointerException npe) {
            fail = true;
        } catch (ResourceAllocationException rae) {
            // too bad
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(id == 1l);
    }
}