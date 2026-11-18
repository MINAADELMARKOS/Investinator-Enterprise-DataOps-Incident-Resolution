import type { Metadata } from "next";
import "@fontsource-variable/orbitron";
import "@fontsource-variable/jetbrains-mono";
import "@fontsource/share-tech-mono/latin-400.css";
import { publicContext } from "@/lib/backend";
import Shell from "@/components/shell";
import "./globals.css";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "InvestiNator — DataOps Control Plane",
  description: "See the signal. Find the cause. Fix with confidence.",
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="en"><body><Shell gateway={await publicContext()}>{children}</Shell></body></html>;
}
