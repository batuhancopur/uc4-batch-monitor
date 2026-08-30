select
  scope_type,
  scope_value
from uc4_report_subscription
where active = true
order by scope_type, scope_value
