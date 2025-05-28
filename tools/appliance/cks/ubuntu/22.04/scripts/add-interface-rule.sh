#!/bin/bash

# File and rule definition
RULE_FILE="/etc/udev/rules.d/90-new-interface.rules"
RULE='ACTION=="add|change|remove", SUBSYSTEM=="net", DRIVERS=="?*", RUN+="/bin/systemctl --no-block start update-netplan.service"'

# Ensure the file exists, or create it
if [[ ! -f $RULE_FILE ]]; then
    touch "$RULE_FILE"
    echo "Created $RULE_FILE."
fi

# Check if the rule already exists to prevent duplication
if grep -Fxq "$RULE" "$RULE_FILE"; then
    echo "Rule already exists in $RULE_FILE."
else
    # Add the rule to the file
    echo "$RULE" | tee -a "$RULE_FILE" > /dev/null
    echo "Rule added to $RULE_FILE."
fi

# Reload udev rules and apply the changes
udevadm control --reload-rules
udevadm trigger
echo "Udev rules reloaded and triggered."