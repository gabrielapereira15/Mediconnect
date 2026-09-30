package com.vegs.mediconnect.backoffice.shared;

import com.vegs.mediconnect.datasource.waitlist.WaitlistEntryRepository;
import com.vegs.mediconnect.datasource.waitlist.WaitlistStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * What every back-office page's shell shows, whichever page it is.
 *
 * The Waitlist count in the side navigation was only ever filled in by
 * Today, so it vanished the moment anyone went anywhere else.
 */
@ControllerAdvice(basePackages = "com.vegs.mediconnect.backoffice")
@RequiredArgsConstructor
public class BackOfficeModelAdvice {

    private final WaitlistEntryRepository waitlistRepository;

    /** People still waiting for something earlier, offers included. */
    @ModelAttribute("waitingCount")
    public int waitingCount() {
        return waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.WAITING).size()
                + waitlistRepository.findAllByStatusOrderByDateCreatedAsc(WaitlistStatus.OFFERED).size();
    }
}
