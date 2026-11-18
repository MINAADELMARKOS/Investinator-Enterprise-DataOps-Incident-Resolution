import OperatorLogin from "@/components/operator-login";
import { PageHeading, Panel } from "@/components/ui";
export default function Login() { return <><PageHeading eyebrow="Operations / Session" title="Operator sign in" description="SIMULATED DEMO · short-lived, role-scoped access" /><Panel title="Demo role session"><OperatorLogin /></Panel></>; }
