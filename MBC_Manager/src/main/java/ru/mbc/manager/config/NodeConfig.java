package ru.mbc.manager.config;

public class NodeConfig {
    public String ip;
    public Integer port;

    @Override
    public int hashCode() {
        return ip.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        NodeConfig cfg = (NodeConfig) obj;
        return this.ip.equals(cfg.ip) && this.port.equals(cfg.port);
    }
}
